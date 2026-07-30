package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.config.VnPayConfig;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.OrderTransactionService;
import com.fpoly.marcusstore.service.AdminNotificationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderCancellationService orderCancellationService;
    private final OrderTransactionService orderTransactionService;
    private final AdminNotificationService notificationService;

    @Transactional
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> receiveIPN(HttpServletRequest request) {

        // ── Bước 1: Thu thập tất cả tham số VNPAY gửi về
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String name = params.nextElement();
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                fields.put(name, value);
            }
        }

        // Tách chữ ký ra khỏi map trước khi băm theo spec VNPAY
        String receivedHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        // ── Bước 2: Tái tạo chuỗi hash và xác thực HmacSHA512
        List<String> sortedKeys = new ArrayList<>(fields.keySet());
        Collections.sort(sortedKeys);

        StringBuilder rawData = new StringBuilder();
        for (Iterator<String> it = sortedKeys.iterator(); it.hasNext();) {
            String key = it.next();
            // Dùng URLEncoder.encode và chuẩn US_ASCII
            rawData.append(key)
                    .append('=')
                    .append(java.net.URLEncoder.encode(fields.get(key), StandardCharsets.US_ASCII));
            if (it.hasNext()) {
                rawData.append('&');
            }
        }

        String computedHash = vnPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), rawData.toString());

        if (!computedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[VNPAY IPN] Chữ ký không hợp lệ. Received={}", receivedHash);
            return ok("97", "Invalid Checksum");
        }

        // ── Bước 3: Lấy các tham số nghiệp vụ
        String orderCode = fields.get("vnp_TxnRef");
        String responseCode = fields.get("vnp_ResponseCode");
        String transactionId = fields.get("vnp_TransactionNo"); // mã GD phía VNPAY
        String amountStr = fields.get("vnp_Amount");

        if (isBlank(orderCode) || isBlank(transactionId)
                || responseCode == null || !responseCode.matches("\\d{2}")) {
            log.warn("[VNPAY IPN] Thiếu tham số nghiệp vụ bắt buộc. TxnRef={}, ResponseCode={}, TransactionNo={}",
                    orderCode, responseCode, transactionId);
            return ok("01", "Invalid Parameters");
        }

        BigDecimal vnpAmount = parseVnPayAmount(amountStr);
        if (vnpAmount == null) {
            log.warn("[VNPAY IPN] Số tiền không hợp lệ. TxnRef={}, Amount={}", orderCode, amountStr);
            return ok("04", "Invalid Amount");
        }

        // ── Bước 4: Tìm đơn hàng trong DB
        Optional<Order> orderOpt = orderRepository.findByOrderCodeForUpdate(orderCode);
        if (orderOpt.isEmpty()) {
            log.warn("[VNPAY IPN] Không tìm thấy đơn hàng: {}", orderCode);
            return ok("01", "Order not found");
        }

        Order order = orderOpt.get();

        // ── Bước 5: Kiểm tra số tiền khớp
        if (order.getFinalAmount() == null || order.getFinalAmount().compareTo(vnpAmount) != 0) {
            log.warn("[VNPAY IPN] Số tiền không khớp. DB={}, VNPAY={}",
                    order.getFinalAmount(), vnpAmount);
            return ok("04", "Invalid Amount");
        }

        // ── Bước 6: Transaction là nguồn idempotency chính, không suy luận từ
        // paymentStatus của Order.
        OrderTransactionService.VnPayTransactionState transactionState = orderTransactionService
                .getVnPayTransactionState(order);
        if (isTerminal(transactionState)) {
            if (isLateSuccessAfterAutomaticCancellation(order, transactionState, responseCode)) {
                handleLateSuccessAfterAutomaticCancellation(order, transactionId, responseCode);
                return ok("00", "Confirm Success");
            }
            if (matchesProcessedCallback(transactionState, transactionId, responseCode)) {
                log.info("[VNPAY IPN] Callback lặp hợp lệ. Order={}, TransactionNo={}, Status={}",
                        orderCode, transactionId, transactionState.status());
            } else {
                log.warn("[VNPAY IPN] Callback xung đột với giao dịch đã chốt. Order={}, "
                        + "IncomingTransactionNo={}, StoredTransactionNo={}, "
                        + "IncomingResponseCode={}, StoredResponseCode={}, Status={}",
                        orderCode,
                        transactionId,
                        transactionState.providerTransactionId(),
                        responseCode,
                        transactionState.responseCode(),
                        transactionState.status());
            }
            return ok("02", "Order already confirmed");
        }

        // ── Bước 7: Cập nhật trạng thái theo kết quả giao dịch
        if ("00".equals(responseCode)) {
            order.setPaymentStatus("PAID");

            // Chỉ set PAID, KHÔNG SET PROCESSING.
            // orderStatus là "PENDING" để Admin vào bấm "Xác nhận đơn"
            order.setOrderStatus("PENDING");

            order.setTransactionId(transactionId);
            order.setPaymentDate(LocalDateTime.now());
            orderTransactionService.markVnPayPaymentSuccess(order, transactionId, responseCode);
            // Marcus sửa lỗi chuông: chỉ IPN thành công mới đưa đơn VNPAY tới admin.
            notificationService.createAndSendNotification(
                    "ORDER",
                    "Đơn VNPAY đã thanh toán: " + orderCode,
                    "Thanh toán đã được VNPAY xác nhận. Đơn hàng sẵn sàng để admin xử lý.",
                    orderCode);

            log.info("[VNPAY IPN] Thanh toán thành công. Đơn hàng {} chuyển sang PENDING để Admin xác nhận.",
                    orderCode);
        } else {
            // Thanh toán thất bại: hủy đơn và hoàn tồn kho/voucher đúng một lần.
            String failureNote = "Giao dịch VNPAY thất bại. TransactionNo: " + transactionId
                    + ", ResponseCode: " + responseCode;
            order.setPaymentStatus("FAILED");
            order.setTransactionId(transactionId);
            orderTransactionService.markVnPayPaymentFailed(
                    order, transactionId, responseCode, failureNote);
            orderCancellationService.cancelAndRestore(order, failureNote);
            log.info("[VNPAY IPN] Thanh toán thất bại/hủy. Order={}", orderCode);
        }

        orderRepository.save(order);
        // Lưu lịch sử trạng thái để Admin biết đơn đã thanh toán
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(order.getOrderStatus());
        if ("00".equals(responseCode)) {
            history.setTitle("Đã thanh toán VNPAY");
            history.setNote("Giao dịch VNPAY thành công: " + transactionId);
        } else {
            history.setTitle("Thanh toán VNPAY thất bại");
            history.setNote("ResponseCode: " + responseCode + ", TransactionNo: " + transactionId);
        }
        orderStatusHistoryRepository.save(history);

        return ok("00", "Confirm Success");
    }

    private boolean isLateSuccessAfterAutomaticCancellation(
            Order order,
            OrderTransactionService.VnPayTransactionState transactionState,
            String responseCode) {
        return "00".equals(responseCode)
                && transactionState != null
                && "FAILED".equalsIgnoreCase(transactionState.status())
                && "CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                && "VNPAY".equalsIgnoreCase(order.getPaymentMethod());
    }

    private void handleLateSuccessAfterAutomaticCancellation(
            Order order, String transactionId, String responseCode) {
        String reason = "VNPAY xác nhận thanh toán thành công sau khi đơn đã tự hủy. TransactionNo: "
                + transactionId;

        orderTransactionService.markLateVnPayPaymentSuccess(order, transactionId, responseCode);
        order.setPaymentStatus("PAID");
        order.setTransactionId(transactionId);
        order.setPaymentDate(LocalDateTime.now());
        orderRepository.save(order);

        // Tài nguyên đã được scheduler hoàn trước đó. Chỉ tạo refund, tuyệt đối
        // không chạy cancelAndRestore lần hai.
        orderCancellationService.requestRefundForCancelledPaidOrder(order, reason);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus("CANCELLED");
        history.setTitle("Thanh toán VNPAY đến muộn");
        history.setNote(reason + ". Hệ thống đã tạo yêu cầu hoàn tiền.");
        orderStatusHistoryRepository.save(history);
        log.warn("[VNPAY IPN] Đơn {} đã tự hủy nhưng VNPAY báo thu tiền thành công; đã tạo refund.",
                order.getOrderCode());
    }

    // tạo response đúng format VNPAY yêu cầu
    private ResponseEntity<Map<String, String>> ok(String rspCode, String message) {
        return ResponseEntity.ok(Map.of("RspCode", rspCode, "Message", message));
    }

    BigDecimal parseVnPayAmount(String amount) {
        if (isBlank(amount) || amount.length() > 30 || !amount.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            // VNPAY gửi số tiền ở đơn vị 1/100 VND.
            BigDecimal parsed = new BigDecimal(amount).movePointLeft(2);
            return parsed.signum() < 0 ? null : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isTerminal(OrderTransactionService.VnPayTransactionState state) {
        return state != null && !"PENDING".equalsIgnoreCase(state.status());
    }

    private boolean matchesProcessedCallback(
            OrderTransactionService.VnPayTransactionState state,
            String transactionId,
            String responseCode) {
        String expectedStatus = "00".equals(responseCode) ? "SUCCESS" : "FAILED";
        return expectedStatus.equalsIgnoreCase(state.status())
                && Objects.equals(transactionId, state.providerTransactionId())
                && Objects.equals(responseCode, state.responseCode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
