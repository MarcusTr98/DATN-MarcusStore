package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ShippingCalculateRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ShippingCalculationResponse;
import com.fpoly.marcusstore.service.GhnService;
import com.fpoly.marcusstore.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/client/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final GhnService ghnService;

    @PostMapping("/calculate")
    public ApiResponse<ShippingCalculationResponse> calculateFee(@Valid @RequestBody ShippingCalculateRequest request) {

        // lấy tổng tiền giỏ hàng chuyển sang Integer để làm phí khai giá (bảo hiểm)
        Integer insuranceValue = request.getCartTotal() != null ? request.getCartTotal().intValue() : 0;

        // 1. Gọi API GHN để lấy phí vận chuyển gốc
        Integer ghnFeeInt = ghnService.calculateShippingFee(
                request.getToDistrictId(),
                request.getToWardCode(),
                request.getTotalWeightGram(),
                insuranceValue);

        // đồng bộ hệ thống tiền tệ
        BigDecimal ghnStandardFee = new BigDecimal(ghnFeeInt.toString());

        // 2. đẩy qua Service tính toán điều kiện Freeship, Chặn đơn, Upsell...
        ShippingCalculationResponse finalResponse = shippingService.calculateFinalShipping(
                request.getCartTotal(),
                ghnStandardFee);

        return ApiResponse.success(finalResponse);
    }
}