package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping
    public ApiResponse<String> processCheckout(@Valid @RequestBody CheckoutRequestDTO requestDTO) {
        Order newOrder = checkoutService.processCheckout(requestDTO);
        return ApiResponse.success("Tạo đơn hàng thành công! Mã đơn: " + newOrder.getOrderCode());
    }
}