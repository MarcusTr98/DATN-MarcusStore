package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderTransactionService {
    private static final String IDEMPOTENCY_KEY_PREFIX = "ORDER";
    private static final String VNPAY_PAYMENT = "VNPAY_PAYMENT";

    private final OrderTransactionRepository transactionRepository;

    @Transactional
    public void recordTransaction(Order order, BigDecimal amount, String type, String status, String note) {
        insertIfAbsent(order, amount, type, status, note);
    }

    @Transactional
    public void markVnPayPaymentSuccess(Order order, String providerTransactionId, String responseCode) {
        markPendingTransactionSuccess(
                order,
                VNPAY_PAYMENT,
                "VNPAY xác nhận thành công. TransactionNo: "
                        + providerTransactionId + ", ResponseCode: " + responseCode);
        updateVnPayProviderResult(order, "SUCCESS", providerTransactionId, responseCode);
    }

    @Transactional
    public void markVnPayPaymentFailed(
            Order order, String providerTransactionId, String responseCode, String note) {
        OrderTransaction transaction = findOrCreateVnPayTransaction(order, note);
        if (transaction == null || "SUCCESS".equalsIgnoreCase(transaction.getStatus())) {
            return;
        }
        transaction.setAmount(order.getFinalAmount());
        transaction.setStatus("FAILED");
        transaction.setNote(note);
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setProviderResponseCode(responseCode);
        transactionRepository.save(transaction);
    }

    // Marcus thêm: scheduler có thể vừa đánh FAILED thì IPN thành công hợp lệ mới
    // đến. Khi đó phải ghi nhận tiền đã thu để tạo refund, không được bỏ callback.
    @Transactional
    public void markLateVnPayPaymentSuccess(
            Order order, String providerTransactionId, String responseCode) {
        OrderTransaction transaction = findOrCreateVnPayTransaction(
                order, "VNPAY xác nhận thành công sau khi đơn đã tự hủy");
        if (transaction == null) {
            throw new IllegalStateException("Không thể ghi nhận giao dịch VNPAY thành công đến muộn");
        }
        transaction.setAmount(order.getFinalAmount());
        transaction.setStatus("SUCCESS");
        transaction.setProviderTransactionId(providerTransactionId);
        transaction.setProviderResponseCode(responseCode);
        transaction.setNote("VNPAY xác nhận thành công sau khi đơn đã tự hủy; chuyển sang chờ hoàn tiền");
        transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public VnPayTransactionState getVnPayTransactionState(Order order) {
        return transactionRepository
                .findFirstByOrder_OrderIdAndTypeOrderByCreatedAtDesc(order.getOrderId(), VNPAY_PAYMENT)
                .map(transaction -> new VnPayTransactionState(
                        transaction.getStatus(),
                        transaction.getProviderTransactionId(),
                        transaction.getProviderResponseCode()))
                .orElse(null);
    }

    @Transactional
    public void recordVnPayPaymentRequestDate(Order order, String transactionDate) {
        OrderTransaction transaction = findOrCreateVnPayTransaction(
                order, "Khởi tạo giao dịch chờ thanh toán VNPAY");
        if (transaction != null && transaction.getProviderTransactionDate() == null) {
            transaction.setProviderTransactionDate(transactionDate);
            transactionRepository.save(transaction);
        }
    }

    @Transactional
    public void markPendingTransactionSuccess(Order order, String type, String note) {
        OrderTransaction transaction = transactionRepository
                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        order.getOrderId(), type, "PENDING")
                .orElse(null);

        if (transaction == null && transactionRepository
                .existsByOrder_OrderIdAndTypeAndStatus(order.getOrderId(), type, "SUCCESS")) {
            return;
        }

        if (transaction == null) {
            insertIfAbsent(order, order.getFinalAmount(), type, "PENDING", note);
            transaction = transactionRepository
                    .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                            order.getOrderId(), type, "PENDING")
                    .orElse(null);
        }

        // Một callback đồng thời khác có thể đã chốt transaction trước khi query lại.
        if (transaction == null) {
            return;
        }

        transaction.setAmount(order.getFinalAmount());
        transaction.setStatus("SUCCESS");
        transaction.setNote(note);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void recordPendingTransactionIfAbsent(
            Order order, BigDecimal amount, String type, String note) {
        insertIfAbsent(order, amount, type, "PENDING", note);
    }

    private boolean insertIfAbsent(
            Order order, BigDecimal amount, String type, String status, String note) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("Đơn hàng phải được lưu trước khi tạo transaction");
        }
        String idempotencyKey = buildIdempotencyKey(order.getOrderId(), type);
        return transactionRepository.insertIfAbsent(
                order.getOrderId(), amount, type, status, note, idempotencyKey) == 1;
    }

    private String buildIdempotencyKey(Integer orderId, String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Loại transaction không được để trống");
        }
        return IDEMPOTENCY_KEY_PREFIX + ":" + orderId + ":" + type.trim().toUpperCase();
    }

    private OrderTransaction findOrCreateVnPayTransaction(Order order, String note) {
        OrderTransaction transaction = transactionRepository
                .findFirstByOrder_OrderIdAndTypeOrderByCreatedAtDesc(order.getOrderId(), VNPAY_PAYMENT)
                .orElse(null);
        if (transaction != null) {
            return transaction;
        }

        insertIfAbsent(order, order.getFinalAmount(), VNPAY_PAYMENT, "PENDING", note);
        return transactionRepository
                .findFirstByOrder_OrderIdAndTypeOrderByCreatedAtDesc(order.getOrderId(), VNPAY_PAYMENT)
                .orElse(null);
    }

    private void updateVnPayProviderResult(
            Order order, String status, String providerTransactionId, String responseCode) {
        transactionRepository
                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        order.getOrderId(), VNPAY_PAYMENT, status)
                .ifPresent(transaction -> {
                    transaction.setProviderTransactionId(providerTransactionId);
                    transaction.setProviderResponseCode(responseCode);
                    transactionRepository.save(transaction);
                });
    }

    public record VnPayTransactionState(
            String status,
            String providerTransactionId,
            String responseCode) {
    }
}
