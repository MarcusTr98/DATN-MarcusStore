package com.fpoly.marcusstore.listener;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.event.OrderConfirmedEvent;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.GhnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation; // IMPORT MỚI
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnOrderListener {

        private final OrderRepository orderRepository;
        private final GhnService ghnService;

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
                        boolean isCod = "COD".equalsIgnoreCase(order.getPaymentMethod());

                        int totalWeight = order.getOrderItems().stream()
                                        .mapToInt(i -> (i.getSku().getWeightGram() > 0 ? i.getSku().getWeightGram()
                                                        : 500) * i.getQuantity())
                                        .sum();

                        // Quy trình chuẩn TMĐT: Shop trả ship (paymentTypeId = 1)
                        int paymentTypeId = 1;

                        GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                                        .paymentTypeId(paymentTypeId)
                                        .serviceTypeId(2)
                                        .note(order.getRecipientName())
                                        .requiredNote("KHONGCHOXEMHANG")
                                        .toName(order.getRecipientName())
                                        .toPhone(order.getRecipientPhone())
                                        .toAddress(order.getShippingAddress())
                                        .toDistrictId(order.getToDistrictId())
                                        .toWardCode(order.getToWardCode())
                                        .weight(totalWeight)
                                        .codAmount(isCod ? order.getFinalAmount().intValue() : 0)
                                        .insuranceValue(order.getTotalAmount().intValue())
                                        .items(order.getOrderItems().stream()
                                                        .map(i -> GhnCreateOrderRequest.Item.builder()
                                                                        .name(i.getSku().getProduct().getProductName())
                                                                        .code(i.getSku().getSkuCode())
                                                                        .quantity(i.getQuantity())
                                                                        .build())
                                                        .collect(Collectors.toList()))
                                        .build();

                        String trackingCode = ghnService.createOrderOnGhn(request);

                        if (trackingCode != null) {
                                order.setTrackingCode(trackingCode);
                                orderRepository.save(order);
                                log.info("✅ Tạo đơn GHN thành công. Tracking: {}", trackingCode);
                        }
                } catch (Exception e) {
                        log.error("❌ Lỗi tạo đơn GHN cho đơn {}: {}", order.getOrderCode(), e.getMessage());
                }
        }
}