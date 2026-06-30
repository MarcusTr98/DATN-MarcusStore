package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderShippingService {
    private final GhnService ghnService;
    private final OrderRepository orderRepository;

    @Transactional
    public void processCreateGhnOrder(Order order) {
        // Logic tách riêng từ GhnOrderListener để dễ quản lý
        int totalWeight = order.getOrderItems().stream()
                .mapToInt(i -> (i.getSku().getWeightGram() > 0 ? i.getSku().getWeightGram() : 500) * i.getQuantity())
                .sum();

        GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                .paymentTypeId(1) // Shop trả phí
                .serviceTypeId(2)
                .toName(order.getRecipientName())
                .toPhone(order.getRecipientPhone())
                .toAddress(order.getShippingAddress())
                .toDistrictId(order.getToDistrictId())
                .toWardCode(order.getToWardCode())
                .weight(totalWeight)
                .codAmount(order.getFinalAmount().intValue())
                .insuranceValue(Math.min(order.getTotalAmount().intValue(), 5000000))
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
        }
    }
}