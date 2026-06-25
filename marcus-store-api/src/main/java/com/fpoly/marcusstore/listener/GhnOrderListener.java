package com.fpoly.marcusstore.listener;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.event.OrderConfirmedEvent;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.GhnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GhnOrderListener {

    private final OrderRepository orderRepository;
    private final GhnService ghnService;

    @Async
    @EventListener
    @Transactional
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
        // LẤY ID TỪ EVENT VÀ FETCH TƯƠI TỪ DB ĐỂ TRÁNH LỖI HIBERNATE
        Integer orderId = event.getOrder().getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (order.getToDistrictId() == null || order.getToWardCode() == null || order.getToWardCode().isBlank()) {
            log.error("❌ Bỏ qua tạo đơn GHN: Đơn hàng {} thiếu thông tin Quận/Huyện/Phường. Data: Dist={}, Ward={}",
                    order.getOrderCode(), order.getToDistrictId(), order.getToWardCode());
            return; // Dừng lại, không gửi request lỗi lên GHN
        }
        log.info("Bắt đầu tạo đơn GHN cho mã: {}", order.getOrderCode());

        try {
            int totalWeight = order.getOrderItems().stream()
                    .mapToInt(
                            i -> (i.getSku().getWeightGram() > 0 ? i.getSku().getWeightGram() : 500) * i.getQuantity())
                    .sum();

            GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                    .paymentTypeId(2)
                    .serviceTypeId(2)
                    .note(order.getRecipientName())
                    .requiredNote("KHONGCHOXEMHANG")
                    .toName(order.getRecipientName())
                    .toPhone(order.getRecipientPhone())
                    .toAddress(order.getShippingAddress())
                    .toDistrictId(order.getToDistrictId())
                    .toWardCode(order.getToWardCode())
                    .weight(totalWeight)
                    .codAmount("COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalAmount().intValue() : 0)
                    .items(order.getOrderItems().stream().map(i -> GhnCreateOrderRequest.Item.builder()
                            .name(i.getSku().getProduct().getProductName())
                            .code(i.getSku().getSkuCode())
                            .quantity(i.getQuantity())
                            .build()).collect(Collectors.toList()))
                    .build();

            String trackingCode = ghnService.createOrderOnGhn(request);

            if (trackingCode != null) {
                order.setTrackingCode(trackingCode);
                orderRepository.save(order);
                log.info("Tạo đơn GHN thành công. Tracking: {}", trackingCode);
            }
        } catch (Exception e) {
            log.error("Lỗi tạo đơn GHN cho đơn {}: {}", order.getOrderCode(), e.getMessage());
        }
    }
}