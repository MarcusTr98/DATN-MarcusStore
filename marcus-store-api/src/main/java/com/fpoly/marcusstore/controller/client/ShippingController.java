package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ShippingCalculateRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ShippingCalculationResponse;
import com.fpoly.marcusstore.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/client/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final CheckoutService checkoutService;

    @PostMapping("/calculate")
    public ApiResponse<ShippingCalculationResponse> calculateFee(@Valid @RequestBody ShippingCalculateRequest request) {

        // Marcus sửa: không tin khối lượng/tổng tiền từ frontend. CheckoutService
        // tính lại theo đúng các CartItem thuộc người dùng hiện tại.
        return ApiResponse.success(checkoutService.calculateShippingForSelection(request));
    }
}
