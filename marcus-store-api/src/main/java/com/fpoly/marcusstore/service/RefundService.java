package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.RefundResponse;
import com.fpoly.marcusstore.dto.response.ClientRefundResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.entity.shopping.RefundRequest;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import com.fpoly.marcusstore.repository.shopping.RefundRequestRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
// Marcus thêm service quản lý state machine, idempotency và số tiền refund
// VNPAY.
public class RefundService {

    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String PROCESSING = "PROCESSING";
    public static final String RETRY_PENDING = "RETRY_PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private static final String VNPAY_PAYMENT = "VNPAY_PAYMENT";
    private static final String REFUND = "REFUND";
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final OrderTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Transactional
    public RefundResponse requestManualRefund(String orderCode, String reason) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        User requestedBy = currentUser();
        return toResponse(createPendingRefund(order, reason, requestedBy));
    }

    @Transactional
    public RefundRequest requestSystemRefundIfEligible(Order order, String reason) {
        if (!isEligible(order)) {
            return null;
        }
        return createPendingRefund(order, reason, null);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> getRefunds(String status, Pageable pageable) {
        String normalized = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                ? null
                : status.trim().toUpperCase();
        return refundRepository.findPage(normalized, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RefundResponse getLatestRefundForOrder(String orderCode) {
        return refundRepository.findFirstByOrder_OrderCodeOrderByCreatedAtDesc(orderCode)
                .map(this::toResponse)
                .orElse(null);
    }

    // Marcus thêm API đọc riêng cho khách hàng, giới hạn theo tài khoản đang đăng
    // nhập.
    @Transactional(readOnly = true)
    public ClientRefundResponse getCurrentUserRefund(String orderCode) {
        return refundRepository
                .findFirstByOrder_OrderCodeAndOrder_User_UserIdOrderByCreatedAtDesc(
                        orderCode, SecurityUtils.getCurrentUserId())
                .map(refund -> ClientRefundResponse.builder()
                        .amount(refund.getAmount())
                        .shippingDeducted(refund.getShippingDeducted())
                        .status(refund.getStatus())
                        .reason(refund.getReason())
                        .createdAt(refund.getCreatedAt())
                        .processedAt(refund.getProcessedAt())
                        .build())
                .orElse(null);
    }

    @Transactional
    public VnPayRefundClient.RefundCommand prepareApproval(Long refundId) {
        RefundRequest refund = lock(refundId);
        if (!PENDING_APPROVAL.equals(refund.getStatus())) {
            throw new RuntimeException("Yêu cầu refund không ở trạng thái chờ duyệt");
        }
        User approver = currentUser();
        // Marcus sửa: yêu cầu chưa từng gửi được sẽ áp dụng chính sách hoàn cả ship
        // mới.
        applyFullRefundPolicy(refund);
        refund.setApprovedBy(approver);
        refund.setApprovedAt(LocalDateTime.now());
        return prepareAttempt(refund, approver.getUsername(), false);
    }

    @Transactional
    public VnPayRefundClient.RefundCommand prepareAdminRetry(Long refundId) {
        RefundRequest refund = lock(refundId);
        // Marcus sửa: admin được chủ động retry cả lỗi đang chờ scheduler sau khi
        // đã khắc phục TLS, vẫn giữ nguyên RequestId chống hoàn trùng.
        if (!(FAILED.equals(refund.getStatus()) || RETRY_PENDING.equals(refund.getStatus()))) {
            throw new RuntimeException("Yêu cầu refund chưa ở trạng thái được phép thử lại");
        }
        User approver = currentUser();
        if (refund.getProviderMessage() != null && refund.getProviderMessage().contains("PKIX")) {
            // Marcus sửa dữ liệu retry cũ: lỗi PKIX xảy ra trước khi request tới VNPAY,
            // nên an toàn cập nhật số tiền sang hoàn toàn bộ theo chính sách mới.
            applyFullRefundPolicy(refund);
        }
        refund.setApprovedBy(approver);
        refund.setApprovedAt(LocalDateTime.now());
        refund.setRetryCount(0);
        refund.setProviderResponseId(null);
        refund.setProviderRefundTransactionId(null);
        refund.setProviderResponseCode(null);
        refund.setProviderTransactionStatus(null);
        refund.setProviderMessage(null);
        // Giữ nguyên RequestId khi retry. Nếu lần gọi trước đã tới VNPAY nhưng client
        // bị timeout, đổi RequestId có thể tạo thêm một lệnh hoàn tiền cho cùng đơn.
        return prepareAttempt(refund, approver.getUsername(), false);
    }

    @Transactional
    public VnPayRefundClient.RefundCommand prepareAutomaticRetry(Long refundId) {
        RefundRequest refund = lock(refundId);
        if (!RETRY_PENDING.equals(refund.getStatus())
                || refund.getRetryCount() >= refund.getMaxRetries()
                || refund.getNextRetryAt() == null
                || refund.getNextRetryAt().isAfter(LocalDateTime.now())) {
            return null;
        }
        return prepareAttempt(refund, "SYSTEM", true);
    }

    @Transactional
    public RefundResponse completeAttempt(Long refundId, VnPayRefundClient.RefundGatewayResult result) {
        RefundRequest refund = lock(refundId);
        copyProviderResult(refund, result);

        switch (result.outcome()) {
            case SUCCESS -> markSuccess(refund);
            case PROCESSING -> markProcessing(refund);
            case RETRYABLE -> markRetryable(refund);
            case FAILED -> markFailed(refund);
        }
        refundRepository.save(refund);
        sendStatusEmailSafely(refund);
        return toResponse(refund);
    }

    @Transactional(readOnly = true)
    public List<Long> findRetryableIds(Pageable limit) {
        return refundRepository.findRetryableIds(LocalDateTime.now(), limit);
    }

    private RefundRequest createPendingRefund(Order order, String reason, User requestedBy) {
        if (!isEligible(order)) {
            throw new RuntimeException("Đơn hàng không đủ điều kiện hoàn tiền VNPAY");
        }
        if (!("CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                || "FAILED".equalsIgnoreCase(order.getOrderStatus()))) {
            throw new RuntimeException("Chỉ tạo refund cho đơn đã hủy hoặc giao thất bại");
        }

        OrderTransaction payment = transactionRepository
                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        order.getOrderId(), VNPAY_PAYMENT, SUCCESS)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch VNPAY thành công"));
        String idempotencyKey = "REFUND:" + order.getOrderId() + ":" + payment.getTransactionId();
        RefundRequest existing = refundRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        // Marcus sửa chính sách: cửa hàng hoặc khách hủy đều hoàn toàn bộ số tiền
        // khách đã thanh toán, bao gồm phí vận chuyển khách thực trả.
        BigDecimal shippingDeducted = BigDecimal.ZERO;
        BigDecimal refundAmount = order.getFinalAmount().max(BigDecimal.ZERO);
        if (refundAmount.signum() <= 0 || refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RuntimeException("Số tiền refund không hợp lệ");
        }

        OrderTransaction refundTransaction = OrderTransaction.builder()
                .order(order)
                .amount(refundAmount)
                .type(REFUND)
                .status("PENDING")
                .note("Chờ admin duyệt hoàn tiền: " + normalizeReason(reason))
                .isReconciled(false)
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(refundTransaction);

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setPaymentTransaction(payment);
        refund.setRefundTransaction(refundTransaction);
        refund.setRequestCode(newRequestCode());
        refund.setIdempotencyKey(idempotencyKey);
        refund.setAmount(refundAmount);
        refund.setShippingDeducted(shippingDeducted);
        refund.setReason(normalizeReason(reason));
        refund.setStatus(PENDING_APPROVAL);
        refund.setRequestedBy(requestedBy);
        refund.setRetryCount(0);
        refund.setMaxRetries(3);

        order.setPaymentStatus("REFUND_PENDING");
        orderRepository.save(order);
        RefundRequest saved = refundRepository.save(refund);
        // Marcus sửa: không gửi mail đồng bộ lúc hủy đơn để giảm độ trễ màn hình admin.
        return saved;
    }

    private void applyFullRefundPolicy(RefundRequest refund) {
        BigDecimal fullAmount = refund.getOrder().getFinalAmount().max(BigDecimal.ZERO);
        if (fullAmount.signum() <= 0
                || fullAmount.compareTo(refund.getPaymentTransaction().getAmount()) > 0) {
            throw new RuntimeException("Số tiền refund toàn phần không hợp lệ");
        }
        refund.setAmount(fullAmount);
        refund.setShippingDeducted(BigDecimal.ZERO);
        refund.getRefundTransaction().setAmount(fullAmount);
    }

    private VnPayRefundClient.RefundCommand prepareAttempt(
            RefundRequest refund, String createBy, boolean automaticRetry) {
        if (automaticRetry || refund.getRetryCount() == 0) {
            refund.setRetryCount(refund.getRetryCount() + 1);
        }
        refund.setStatus(PROCESSING);
        refund.setLastAttemptAt(LocalDateTime.now());
        refund.setNextRetryAt(null);
        refundRepository.save(refund);

        OrderTransaction payment = refund.getPaymentTransaction();
        String transactionDate = payment.getProviderTransactionDate();
        if (transactionDate == null || !transactionDate.matches("\\d{14}")) {
            LocalDateTime fallback = payment.getCreatedAt() != null
                    ? payment.getCreatedAt()
                    : refund.getOrder().getCreatedAt();
            transactionDate = fallback.format(VNPAY_DATE);
        }
        return new VnPayRefundClient.RefundCommand(
                refund.getRefundId(),
                refund.getRequestCode(),
                refund.getOrder().getOrderCode(),
                refund.getAmount(),
                payment.getProviderTransactionId(),
                transactionDate,
                createBy);
    }

    private void markSuccess(RefundRequest refund) {
        refund.setStatus(SUCCESS);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setNextRetryAt(null);
        refund.getOrder().setPaymentStatus("REFUNDED");
        refund.getRefundTransaction().setStatus("SUCCESS");
        refund.getRefundTransaction().setProviderTransactionId(refund.getProviderRefundTransactionId());
        refund.getRefundTransaction().setProviderResponseCode(refund.getProviderResponseCode());
        refund.getRefundTransaction().setNote("Hoàn tiền VNPAY thành công");
    }

    private void markProcessing(RefundRequest refund) {
        refund.setStatus(PROCESSING);
        refund.setNextRetryAt(null);
        refund.getOrder().setPaymentStatus("REFUND_PENDING");
        refund.getRefundTransaction().setStatus("PENDING");
        refund.getRefundTransaction().setNote("VNPAY đang xử lý yêu cầu hoàn tiền");
    }

    private void markRetryable(RefundRequest refund) {
        if (refund.getRetryCount() < refund.getMaxRetries()) {
            refund.setStatus(RETRY_PENDING);
            long delayMinutes = switch (refund.getRetryCount()) {
                case 1 -> 1;
                case 2 -> 5;
                default -> 15;
            };
            refund.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            refund.getOrder().setPaymentStatus("REFUND_PENDING");
            return;
        }
        markFailed(refund);
    }

    private void markFailed(RefundRequest refund) {
        refund.setStatus(FAILED);
        refund.setProcessedAt(LocalDateTime.now());
        refund.setNextRetryAt(null);
        refund.getOrder().setPaymentStatus("REFUND_FAILED");
        refund.getRefundTransaction().setStatus("FAILED");
        refund.getRefundTransaction().setNote("Hoàn tiền VNPAY thất bại: " + refund.getProviderMessage());
    }

    private void copyProviderResult(RefundRequest refund, VnPayRefundClient.RefundGatewayResult result) {
        refund.setProviderResponseCode(result.responseCode());
        refund.setProviderTransactionStatus(result.transactionStatus());
        refund.setProviderMessage(result.message());
        refund.setProviderResponseId(result.responseId());
        refund.setProviderRefundTransactionId(result.refundTransactionId());
    }

    private boolean isEligible(Order order) {
        return order != null
                && "VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                && ("PAID".equalsIgnoreCase(order.getPaymentStatus())
                        || "REFUND_PENDING".equalsIgnoreCase(order.getPaymentStatus())
                        || "REFUND_FAILED".equalsIgnoreCase(order.getPaymentStatus()));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại"));
    }

    private RefundRequest lock(Long refundId) {
        return refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu refund"));
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Hoàn tiền đơn hàng VNPAY";
        }
        String normalized = reason.trim();
        return normalized.substring(0, Math.min(500, normalized.length()));
    }

    private String newRequestCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void sendStatusEmailSafely(RefundRequest refund) {
        try {
            emailService.sendRefundStatusUpdate(
                    refund.getOrder().getUser().getEmail(),
                    refund.getOrder().getUser().getFullName(),
                    refund.getOrder(),
                    refund.getAmount(),
                    refund.getStatus());
        } catch (Exception ignored) {
            // Email không được làm rollback nghiệp vụ tài chính.
        }
    }

    private RefundResponse toResponse(RefundRequest refund) {
        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .orderCode(refund.getOrder().getOrderCode())
                .paymentTransactionId(refund.getPaymentTransaction().getTransactionId())
                .refundTransactionId(refund.getRefundTransaction() == null
                        ? null
                        : refund.getRefundTransaction().getTransactionId())
                .requestCode(refund.getRequestCode())
                .amount(refund.getAmount())
                .shippingDeducted(refund.getShippingDeducted())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .requestedBy(displayName(refund.getRequestedBy()))
                .approvedBy(displayName(refund.getApprovedBy()))
                .retryCount(refund.getRetryCount())
                .maxRetries(refund.getMaxRetries())
                .providerRefundTransactionId(refund.getProviderRefundTransactionId())
                .providerResponseCode(refund.getProviderResponseCode())
                .providerTransactionStatus(refund.getProviderTransactionStatus())
                .providerMessage(refund.getProviderMessage())
                .createdAt(refund.getCreatedAt())
                .approvedAt(refund.getApprovedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }

    private String displayName(User user) {
        if (user == null) {
            return "SYSTEM";
        }
        return user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername()
                : user.getFullName();
    }
}
