package com.fpoly.marcusstore.listener;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.event.OrderConfirmedEvent;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.OrderShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation; // IMPORT MỚI
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnOrderListener {

        private final OrderRepository orderRepository;
        private final OrderShippingService orderShippingService;

        @Async
        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        // FIX: Buộc mở một transaction mới độc lập để ghi dữ liệu sau khi luồng chính
        // đã commit
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
                Integer orderId = event.getOrder().getOrderId();
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                // Chống sinh trùng đơn nếu bị spam
                if (order.getTrackingCode() != null && !order.getTrackingCode().isBlank()) {
                        log.warn("⚠️ Bỏ qua tạo đơn GHN: Đơn {} đã có mã vận đơn {}", order.getOrderCode(),
                                        order.getTrackingCode());
                        return;
                }

                if (order.getToDistrictId() == null || order.getToWardCode() == null
                                || order.getToWardCode().isBlank()) {
                        log.error("❌ Bỏ qua tạo đơn GHN: Đơn hàng {} thiếu thông tin địa chỉ.", order.getOrderCode());
                        return;
                }

                log.info("🚀 Bắt đầu tạo đơn GHN cho mã: {}", order.getOrderCode());

                try {
                        orderShippingService.processCreateGhnOrder(order);
                        log.info("✅ Tạo đơn GHN thành công. Tracking: {}", order.getTrackingCode());
                } catch (Exception e) {
                        log.error("❌ Lỗi tạo đơn GHN cho đơn {}: {}", order.getOrderCode(), e.getMessage());
                }
        }
}
