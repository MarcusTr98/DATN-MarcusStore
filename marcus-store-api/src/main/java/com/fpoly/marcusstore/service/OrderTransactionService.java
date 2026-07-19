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

    private final OrderTransactionRepository transactionRepository;

    @Transactional
    public void recordTransaction(Order order, BigDecimal amount, String type, String status, String note) {
        insertIfAbsent(order, amount, type, status, note);
    }

    @Transactional
    public void markVnPayPaymentSuccess(Order order, String providerTransactionId, String responseCode) {
        markPendingTransactionSuccess(
                order,
                "VNPAY_PAYMENT",
                "VNPAY xác nhận thành công. TransactionNo: "
                        + providerTransactionId + ", ResponseCode: " + responseCode);
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
}
