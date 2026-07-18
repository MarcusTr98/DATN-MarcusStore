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
        // Chốt chính transaction PENDING đã tạo lúc checkout, không tạo dòng trùng.
        transactionService.markPendingTransactionSuccess(
                order,
                type,
                "Giao dịch hoàn tất, Ref: " + refCode);
    }
}
