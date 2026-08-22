package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AssignOrderRequest;
import com.fpoly.marcusstore.dto.request.StaffAssignmentSettingsRequest;
import com.fpoly.marcusstore.dto.response.StaffAssignmentStatusResponse;
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
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class AdminOrderAssignmentController {
    private final OrderAssignmentService orderAssignmentService;
    private final OrderService orderService;

    @GetMapping("/dashboard")
    public OrderAssignmentDashboardResponse getDashboard(
            @RequestParam(defaultValue = "0") int pendingPage,
            @RequestParam(defaultValue = "5") int pendingSize) {
        return orderAssignmentService.getDashboard(
                Math.max(0, pendingPage), Math.max(1, Math.min(pendingSize, 50)));
    }

    @PutMapping("/{orderCode}")
    public OrderDetailResponse assignOrder(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody AssignOrderRequest request) {
        return orderService.assignOrderToStaff(orderCode, request.getStaffId(), request.getReason());
    }

    @PutMapping("/staff/{staffId}/settings")
    public StaffAssignmentStatusResponse updateStaffSettings(@PathVariable Integer staffId,
            @Valid @RequestBody StaffAssignmentSettingsRequest request) {
        return orderAssignmentService.updateStaffSettings(
                staffId, request.getAcceptingOrders(), request.getMaxActiveOrders());
    }
}
