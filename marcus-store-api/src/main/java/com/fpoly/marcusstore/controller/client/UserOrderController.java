package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.service.OrderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
