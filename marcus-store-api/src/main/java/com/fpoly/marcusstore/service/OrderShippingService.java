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
import java.math.BigDecimal;
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

                boolean isVnPay = "VNPAY".equalsIgnoreCase(order.getPaymentMethod());
                boolean isPaid = "PAID".equalsIgnoreCase(order.getPaymentStatus());

                if (isVnPay && !isPaid) {
                        throw new IllegalStateException(
                                        "Đơn VNPAY chưa thanh toán thành công, không thể tạo vận đơn GHN");
                }

                int codAmount = calculateCodAmount(order, isPaid);

                // 1. Lấy giới hạn bảo hiểm từ DB
                ShippingConfig config = shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                                .orElse(null);

                BigDecimal maxInsuranceLimit = config != null && config.getMaxInsuranceValue() != null
                                ? config.getMaxInsuranceValue()
                                : new BigDecimal("5000000");
                int insuranceValue = calculateInsuranceValue(order.getTotalAmount(), maxInsuranceLimit);

                // 2. Tính toán tổng khối lượng
                int totalWeight = order.getOrderItems().stream()
                                .mapToInt(i -> {
                                        Integer weightGram = i.getSku().getWeightGram();
                                        int safeWeight = weightGram != null && weightGram > 0 ? weightGram : 500;
                                        return safeWeight * i.getQuantity();
                                })
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
                                .codAmount(codAmount)
                                // Chỉ giới hạn tiền bảo hiểm, không giới hạn giá trị đơn hàng.
                                .insuranceValue(insuranceValue)
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

        private int calculateCodAmount(Order order, boolean isPaid) {
                if (!"COD".equalsIgnoreCase(order.getPaymentMethod()) || isPaid) {
                        return 0;
                }

                try {
                        return order.getFinalAmount().intValueExact();
                } catch (ArithmeticException e) {
                        throw new IllegalArgumentException(
                                        "Số tiền thu hộ COD vượt giới hạn hỗ trợ của GHN", e);
                }
        }

        private int calculateInsuranceValue(BigDecimal orderAmount, BigDecimal configuredLimit) {
                if (orderAmount == null || orderAmount.signum() <= 0
                                || configuredLimit == null || configuredLimit.signum() <= 0) {
                        return 0;
                }

                BigDecimal integerLimit = BigDecimal.valueOf(Integer.MAX_VALUE);
                BigDecimal safeLimit = configuredLimit.min(integerLimit);
                return orderAmount.min(safeLimit).intValue();
        }
}
