package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import com.fpoly.marcusstore.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAuthority('ORDER_VIEW')")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public Page<OrderResponse> getOrders(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderService.getOrdersPage(keyword, paymentMethod, orderStatus, fromDate, toDate, pageable);
    }

    @GetMapping("/stats")
    public OrderStatsResponse getOrderStats(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String orderStatus) {
        return orderService.getOrderStats(keyword, paymentMethod, orderStatus);
    }

    @GetMapping("/filter-options")
    public Map<String, Object> getOrderFilterOptions() {
        return Map.of("paymentMethods", orderService.getPaymentMethods(),
                "orderStatuses", orderService.getOrderStatuses());
    }

    @GetMapping("/{orderCode}")
    public OrderDetailResponse getOrder(@PathVariable @Size(min = 1, max = 50) String orderCode) {
        return orderService.getOrderDetailResponse(orderCode);
    }

    @PutMapping("/{orderCode}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public OrderDetailResponse updateOrderStatus(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatusOrder(orderCode, request);
    }
}
