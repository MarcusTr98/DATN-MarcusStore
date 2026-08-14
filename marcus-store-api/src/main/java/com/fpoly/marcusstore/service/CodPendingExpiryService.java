package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marcus thêm: giải phóng tài nguyên của đơn COD bị bỏ quên ở trạng thái chờ xác
 * nhận. Service khóa từng đơn để không chạy song song với Admin xác nhận/hủy.
 */
@Service
@RequiredArgsConstructor
public class CodPendingExpiryService {

    static final String REASON_CODE = "SYSTEM_COD_CONFIRMATION_EXPIRED";
    static final String REASON = "Hệ thống tự hủy vì đơn COD chờ xác nhận quá thời gian cho phép";

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderCancellationService cancellationService;
    private final UserNotificationService userNotificationService;

    @Transactional
    public boolean cancelOneExpiredCod(Integer orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (!isStillPendingCod(order)) {
            return false;
        }

        if (!cancellationService.cancelAndRestore(order, REASON_CODE, "SYSTEM", REASON)) {
            return false;
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus("CANCELLED");
        history.setTitle("Tự hủy đơn COD quá hạn xác nhận");
        history.setNote(REASON);
        historyRepository.save(history);

        userNotificationService.createOrderStatusNotification(
                order, "CANCELLED",
                "Đơn " + order.getOrderCode() + " đã tự hủy vì chờ xác nhận quá lâu.");
        return true;
    }

    private boolean isStillPendingCod(Order order) {
        return order != null
                && "COD".equalsIgnoreCase(order.getPaymentMethod())
                && "UNPAID".equalsIgnoreCase(order.getPaymentStatus())
                && "PENDING".equalsIgnoreCase(order.getOrderStatus());
    }
}
