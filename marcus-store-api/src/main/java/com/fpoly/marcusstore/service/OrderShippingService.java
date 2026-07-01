package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.core.ShippingConfig;
import com.fpoly.marcusstore.repository.core.ShippingConfigRepository;
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
        private final ShippingConfigRepository shippingConfigRepository;

        @Transactional
        public void processCreateGhnOrder(Order order) {

                // 1. Lấy giới hạn bảo hiểm từ DB
                ShippingConfig config = shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                                .orElse(null);

                int maxInsuranceLimit = 5000000; // Giá trị fallback an toàn
                if (config != null && config.getMaxInsuranceValue() != null) {
                        maxInsuranceLimit = config.getMaxInsuranceValue().intValue();
                }

                // 2. Tính toán tổng khối lượng
                int totalWeight = order.getOrderItems().stream()
                                .mapToInt(i -> (i.getSku().getWeightGram() > 0 ? i.getSku().getWeightGram() : 500)
                                                * i.getQuantity())
                                .sum();

                // 3. Khởi tạo Request với dữ liệu động
                GhnCreateOrderRequest request = GhnCreateOrderRequest.builder()
                                .paymentTypeId(1) // Shop trả phí (1)
                                .serviceTypeId(2)
                                .requiredNote("KHONGCHOXEMHANG")
                                .toName(order.getRecipientName())
                                .toPhone(order.getRecipientPhone())
                                .toAddress(order.getShippingAddress())
                                .toDistrictId(order.getToDistrictId())
                                .toWardCode(order.getToWardCode())
                                .weight(totalWeight)
                                .codAmount(order.getFinalAmount().intValue())
                                // Áp dụng giới hạn động từ Database
                                .insuranceValue(Math.min(order.getTotalAmount().intValue(), maxInsuranceLimit))
                                .items(order.getOrderItems().stream()
                                                .map(i -> GhnCreateOrderRequest.Item.builder()
                                                                .name(i.getSku().getProduct().getProductName())
                                                                .code(i.getSku().getSkuCode())
                                                                .quantity(i.getQuantity())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();

                // 4. đẨY sang GHN và lưu Tracking Code
                String trackingCode = ghnService.createOrderOnGhn(request);
                if (trackingCode != null) {
                        order.setTrackingCode(trackingCode);
                        orderRepository.save(order);
                }
        }
}