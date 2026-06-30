package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {
    private final OrderTransactionService transactionService;

    @Transactional
    public void handlePaymentSuccess(Order order, String type, BigDecimal amount, String refCode) {
        // Cập nhật trạng thái thanh toán
        order.setPaymentStatus("PAID");
        // Ghi nhận vào bảng Transaction để đối soát
        transactionService.recordTransaction(
                order,
                amount,
                type, // VNPAY_PAYMENT hoặc COD_COLLECTION
                "SUCCESS",
                "Giao dịch thành công, Ref: " + refCode);
    }
}