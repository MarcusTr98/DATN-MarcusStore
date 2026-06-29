package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ShippingCalculationResponse;
import com.fpoly.marcusstore.entity.core.ShippingConfig;
import com.fpoly.marcusstore.repository.core.ShippingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShippingConfigRepository shippingConfigRepository;

    public ShippingCalculationResponse calculateFinalShipping(BigDecimal cartTotal, BigDecimal ghnStandardFee) {

        // 1. Lấy cấu hình (Fallback an toàn nếu DB trống)
        ShippingConfig config = shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    ShippingConfig fallback = new ShippingConfig();
                    fallback.setMinOrderValue(BigDecimal.ZERO);
                    fallback.setThresholdValue(new BigDecimal("999999999"));
                    return fallback;
                });

        // CÔNG CỤ FORMAT SỐ TIỀN CHUẨN VIỆT NAM (VD: 200.000đ)
        DecimalFormat formatter = new DecimalFormat("#,###");

        // 2. Tầng 1: Kiểm tra "Chốt chặn"
        if (cartTotal.compareTo(config.getMinOrderValue()) < 0) {
            String formattedMin = formatter.format(config.getMinOrderValue());
            return ShippingCalculationResponse.builder()
                    .isAllowedToOrder(false)
                    .blockMessage("Đơn hàng tối thiểu để đặt hàng là " + formattedMin + "đ.")
                    .standardShippingFee(ghnStandardFee)
                    .discountedShippingFee(ghnStandardFee)
                    .isFreeship(false)
                    .amountUntilFreeship(BigDecimal.ZERO)
                    .suggestionMessage("")
                    .build();
        }

        // 3. Tầng 2 & 3: Tính toán Freeship và Upsell
        boolean isFreeship = cartTotal.compareTo(config.getThresholdValue()) >= 0;
        BigDecimal discountedFee = isFreeship ? BigDecimal.ZERO : ghnStandardFee;

        BigDecimal amountUntilFreeship = BigDecimal.ZERO;
        String suggestionMessage = "Tuyệt vời! Đơn hàng của bạn đã được MIỄN PHÍ vận chuyển.";

        if (!isFreeship) {
            amountUntilFreeship = config.getThresholdValue().subtract(cartTotal);
            String formattedMissing = formatter.format(amountUntilFreeship);
            suggestionMessage = "Mua thêm " + formattedMissing + "đ nữa để được MIỄN PHÍ vận chuyển!";
        }

        // 4. Trả kết quả
        return ShippingCalculationResponse.builder()
                .isAllowedToOrder(true)
                .blockMessage("")
                .standardShippingFee(ghnStandardFee)
                .discountedShippingFee(discountedFee)
                .isFreeship(isFreeship)
                .amountUntilFreeship(amountUntilFreeship)
                .suggestionMessage(suggestionMessage)
                .build();
    }
}