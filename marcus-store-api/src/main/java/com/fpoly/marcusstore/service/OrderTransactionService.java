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
    private final OrderTransactionRepository transactionRepository;

    @Transactional
    public void recordTransaction(Order order, BigDecimal amount, String type, String status, String note) {
        OrderTransaction trans = OrderTransaction.builder()
                .order(order)
                .amount(amount)
                .type(type)
                .status(status)
                .note(note)
                .isReconciled(false)
                .build();
        transactionRepository.save(trans);
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
            transaction = OrderTransaction.builder()
                    .order(order)
                    .amount(order.getFinalAmount())
                    .type(type)
                    .status("PENDING")
                    .isReconciled(false)
                    .build();
        }

        transaction.setAmount(order.getFinalAmount());
        transaction.setStatus("SUCCESS");
        transaction.setNote(note);
        transactionRepository.save(transaction);
    }
}
