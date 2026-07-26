package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VnPayPaymentExpiryService {

    private static final String EXPIRED_REASON = "Hệ thống tự hủy vì giao dịch VNPAY không hoàn tất trong thời gian cho phép";

    private final OrderRepository orderRepository;
    private final OrderTransactionRepository transactionRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderCancellationService cancellationService;

    @Transactional
    public void cancelOneExpiredPayment(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (!isStillAwaitingVnPay(order)) {
            return;
        }

        // Marcus sửa: gắn FAILED trước khi hủy để màn hình khách/admin không còn hiện
        // treo.
        order.setPaymentStatus("FAILED");
        if (!cancellationService.cancelAndRestore(order, EXPIRED_REASON)) {
            return;
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus("CANCELLED");
        history.setTitle("Tự hủy do quá hạn thanh toán VNPAY");
        history.setNote(EXPIRED_REASON);
        historyRepository.save(history);
    }

    private boolean isStillAwaitingVnPay(Order order) {
        return order != null
                && "VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                && "PENDING".equalsIgnoreCase(order.getPaymentStatus())
                && "PENDING".equalsIgnoreCase(order.getOrderStatus())
                && transactionRepository
                        .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                order.getOrderId(), "VNPAY_PAYMENT", "PENDING")
                        .isPresent();
    }
}
