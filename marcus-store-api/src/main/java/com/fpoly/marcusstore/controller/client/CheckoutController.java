package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.CalculateFeeRequestDTO;
import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.service.CheckoutService;
import com.fpoly.marcusstore.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private VnPayService vnPayService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> processCheckout(
            @Valid @RequestBody CheckoutRequestDTO request, // Đã bổ sung @Valid
            HttpServletRequest httpRequest) {

        // 1. Xử lý nghiệp vụ và lưu đơn hàng
        Order savedOrder = checkoutService.processCheckout(request);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("orderCode", savedOrder.getOrderCode());

        // Marcus sửa: response retry phải theo đơn đã lưu, không tin paymentMethod
        // client gửi lại. Chỉ phát URL khi giao dịch vẫn đang chờ thanh toán.
        if ("VNPAY".equalsIgnoreCase(savedOrder.getPaymentMethod())
                && "PENDING".equalsIgnoreCase(savedOrder.getPaymentStatus())) {
            String paymentUrl = vnPayService.createPaymentUrl(savedOrder, httpRequest);
            responseData.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(new ApiResponse<>(200, "Tạo URL VNPAY thành công", responseData));
        }

        // 3. Nếu là COD hoặc PayOS => Trả về data bình thường
        return ResponseEntity.ok(new ApiResponse<>(200, "Đặt hàng thành công", responseData));
    }

    @PostMapping("/calculate-fee")
    public ApiResponse<Integer> calculateFee(@Valid @RequestBody CalculateFeeRequestDTO requestDTO) {
        Integer fee = checkoutService.calculateShippingFeeForCart(requestDTO);
        return ApiResponse.success(fee);
    }
}
