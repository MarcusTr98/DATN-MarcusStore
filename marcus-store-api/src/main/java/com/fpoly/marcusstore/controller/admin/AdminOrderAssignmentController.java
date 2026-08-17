package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AssignOrderRequest;
import com.fpoly.marcusstore.dto.response.OrderAssignmentDashboardResponse;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.service.OrderAssignmentService;
import com.fpoly.marcusstore.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/order-assignments")
@PreAuthorize("hasAuthority('ORDER_VIEW')")
@RequiredArgsConstructor
@Validated
public class AdminOrderAssignmentController {
    private final OrderAssignmentService orderAssignmentService;
    private final OrderService orderService;

    @GetMapping("/dashboard")
    public OrderAssignmentDashboardResponse getDashboard() {
        return orderAssignmentService.getDashboard();
    }

    @PutMapping("/{orderCode}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public OrderDetailResponse assignOrder(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody AssignOrderRequest request) {
        return orderService.assignOrderToStaff(orderCode, request.getStaffId(), request.getReason());
    }
}
