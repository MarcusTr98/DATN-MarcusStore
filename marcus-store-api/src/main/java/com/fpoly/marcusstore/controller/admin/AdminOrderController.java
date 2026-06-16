package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import com.fpoly.marcusstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping("/orders")
    public Page<OrderResponse> getAllOrder(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String paymentMethod,
                                           @RequestParam(required = false) String orderStatus) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "orderId"));
        return orderService.getOrdersPage(keyword, paymentMethod, orderStatus, pageable);
    }
    @GetMapping("/orders/stats")
    public OrderStatsResponse getOrderStats(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String paymentMethod,
                                            @RequestParam(required = false) String orderStatus){
        return orderService.getOrderStats(keyword, paymentMethod, orderStatus);
    }
    @GetMapping("/orders/filter-options")
    public Map<String, Object> getOrderFilterOptions() {
        return Map.of(
                "paymentMethods", orderService.getPaymentMethods(),
                "orderStatuses", orderService.getOrderStatuses()
        );
    }
}
