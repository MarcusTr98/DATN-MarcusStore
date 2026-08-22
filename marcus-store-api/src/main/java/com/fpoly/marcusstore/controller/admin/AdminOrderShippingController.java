package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.service.OrderService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders/{orderCode}/shipment")
@PreAuthorize("hasAuthority('ORDER_UPDATE')")
@RequiredArgsConstructor
@Validated
public class AdminOrderShippingController {
    private final OrderService orderService;

    @PostMapping("/retry")
    public OrderDetailResponse retryGhn(@PathVariable @Size(min = 1, max = 50) String orderCode) {
        return orderService.retryGhnShipment(orderCode);
    }
}
