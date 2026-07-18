package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    private final OrderTransactionService transactionService;

    @Transactional
    public void handlePaymentSuccess(Order order, String type, String refCode) {
        // Cập nhật trạng thái thanh toán
        order.setPaymentStatus("PAID");
        if (order.getPaymentDate() == null) {
            order.setPaymentDate(LocalDateTime.now());
        }
        transactionService.markPendingTransactionSuccess(
                order,
                type,
                "Giao dịch hoàn tất, Ref: " + refCode);
    }

    @Transactional
    public void handleCodDelivered(Order order, String refCode) {
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return;
        }
        handlePaymentSuccess(order, "COD_COLLECTION", refCode);
    }
}
