package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderAutoCompletionService {

        private final OrderRepository orderRepository;
        private final OrderStatusHistoryRepository historyRepository;
        private final OrderPaymentService orderPaymentService;
        private final UserNotificationService userNotificationService;
        private final AdminNotificationService adminNotificationService;

        // Marcus thêm: mỗi đơn được khóa và kiểm tra lại trong transaction riêng để
        // scheduler không đè lên thao tác xác nhận đồng thời của khách/Admin.
        @Transactional
        public boolean completeDeliveredOrder(Integer orderId) {
                Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
                if (order == null
                                || !"DELIVERED".equalsIgnoreCase(order.getOrderStatus())
                                || "STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod())) {
                        return false;
                }

                if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                                && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                        return false;
                }

                String transactionType = "COD".equalsIgnoreCase(order.getPaymentMethod())
                                ? "COD_COLLECTION"
                                : "VNPAY_PAYMENT";
                orderPaymentService.handlePaymentSuccess(
                                order,
                                transactionType,
                                "AUTO_COMPLETED_AFTER_DELIVERED:" + order.getOrderCode());
                order.setOrderStatus("COMPLETED");
                orderRepository.save(order);

                OrderStatusHistory history = new OrderStatusHistory();
                history.setOrder(order);
                history.setStatus("COMPLETED");
                history.setTitle("Đơn hàng hoàn thành");
                history.setNote("Hệ thống tự hoàn thành sau thời gian chờ khách xác nhận nhận hàng");
                // createdBy để null nhằm thể hiện đây là thao tác hệ thống.
                historyRepository.save(history);

                userNotificationService.createOrderStatusNotification(
                                order,
                                "COMPLETED",
                                "Đơn " + order.getOrderCode()
                                                + " đã tự động hoàn thành sau thời gian xác nhận giao hàng.");
                adminNotificationService.createAndSendNotification(
                                "ORDER_COMPLETED",
                                "Đơn " + order.getOrderCode() + " đã tự hoàn thành",
                                "Hệ thống tự hoàn thành đơn sau khi GHN giao thành công và hết thời gian chờ khách xác nhận.",
                                order.getOrderCode());
                return true;
        }
}
