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

        // 1. Lấy cấu hình, Fallback an toàn nếu DB trống
        ShippingConfig config = shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    ShippingConfig fallback = new ShippingConfig();
                    fallback.setMinOrderValue(BigDecimal.ZERO);
                    fallback.setThresholdValue(new BigDecimal("999999999"));
                    fallback.setMaxFreeshipAmount(BigDecimal.ZERO);
                    fallback.setMaxInsuranceValue(new BigDecimal("5000000"));
                    return fallback;
                });

        DecimalFormat formatter = new DecimalFormat("#,###");

        // 2. Tầng 1: Kiểm tra Min Order Value
        if (config.getMinOrderValue() != null && cartTotal.compareTo(config.getMinOrderValue()) < 0) {
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

        // 3. Tầng 2 & 3: Tính toán Trợ giá và Upsell
        boolean isEligibleForSubsidy = cartTotal.compareTo(config.getThresholdValue()) >= 0;

        BigDecimal discountedFee;
        BigDecimal amountUntilFreeship = BigDecimal.ZERO;
        String suggestionMessage;

        // Giá trị trợ giá an toàn (tránh NullPointerException)
        BigDecimal maxSubsidy = config.getMaxFreeshipAmount() != null ? config.getMaxFreeshipAmount()
                : new BigDecimal("60000");

        if (isEligibleForSubsidy) {
            // Nếu đủ điều kiện trợ giá: Kiểm tra xem phí GHN có vượt mức trợ giá không
            if (ghnStandardFee.compareTo(maxSubsidy) <= 0) {
                // Phí GHN nhỏ hơn hoặc bằng 60k => Khách được freeship 100%
                discountedFee = BigDecimal.ZERO;
                suggestionMessage = "Tuyệt vời! Đơn hàng của bạn đã được MIỄN PHÍ vận chuyển.";
            } else {
                // Phí GHN lớn hơn 60k -> Khách phải trả phần chênh lệch
                discountedFee = ghnStandardFee.subtract(maxSubsidy);
                suggestionMessage = "Đơn hàng đã được hỗ trợ " + formatter.format(maxSubsidy) + "đ phí vận chuyển.";
            }
        } else {
            // Không đủ điều kiện => Khách trả full phí
            discountedFee = ghnStandardFee;
            amountUntilFreeship = config.getThresholdValue().subtract(cartTotal);
            String formattedMissing = formatter.format(amountUntilFreeship);
            suggestionMessage = "Mua thêm " + formattedMissing + "đ nữa để được hỗ trợ vận chuyển!";
        }

        // isFreeship thực tế chỉ True khi discountedFee bằng 0
        boolean isFreeship = discountedFee.compareTo(BigDecimal.ZERO) == 0;

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