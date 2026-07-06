package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.service.OrderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class UserOrderController {
    private final OrderService orderService;
    @GetMapping
    public List<OrderResponse> getAllOrder(){
        return orderService.getUserOrder();
    }
    @GetMapping("/{orderCode}")
    public OrderDetailResponse getUserOrderDetail(@PathVariable("orderCode") String orderCode){
        return orderService.getUserOrderDetail(orderCode);
    }

    @PostMapping("/{orderCode}/cancel")
    public OrderDetailResponse cancelUserOrder(
            @PathVariable("orderCode") String orderCode,
            @RequestBody(required = false) UpdateOrderStatusRequest request) {
        String reason = request == null ? null : request.getNote();
        return orderService.cancelUserOrder(orderCode, reason);
    }
}
