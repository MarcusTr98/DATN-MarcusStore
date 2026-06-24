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
        Order order = event.getOrder();
        log.info("🚀 Bắt đầu tạo đơn GHN cho mã: {}", order.getOrderCode());

        try {
            // Mapping Items
            var ghnItems = order.getOrderItems().stream().map(i -> GhnCreateOrderRequest.Item.builder()
                    .name(i.getSku().getProduct().getProductName())
                    .code(i.getSku().getSkuCode())
                    .quantity(i.getQuantity())
                    .build()).collect(Collectors.toList());

            // Build Request
            GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                    .paymentTypeId(2) // 2: Khách trả ship
                    .note(order.getRecipientName())
                    .requiredNote("KHONGCHOXEMHANG")
                    .toName(order.getRecipientName())
                    .toPhone(order.getRecipientPhone())
                    .toAddress(order.getShippingAddress())
                    .toDistrictId(order.getToDistrictId())
                    .toWardCode(order.getToWardCode())
                    // Tính trọng lượng (default 500g/sp nếu weightGram = 0)
                    .weight(order.getOrderItems().stream()
                            .mapToInt(i -> (i.getSku().getWeightGram() > 0 ? i.getSku().getWeightGram() : 500)
                                    * i.getQuantity())
                            .sum())
                    // Nếu là COD thì truyền finalAmount, không thì truyền 0
                    .codAmount("COD".equalsIgnoreCase(order.getPaymentMethod()) ? order.getFinalAmount().intValue() : 0)
                    .items(ghnItems)
                    .build();

            // Gọi API tạo đơn
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