package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.StaffAvailabilityRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.StaffAssignmentStatusResponse;
import com.fpoly.marcusstore.service.OrderAssignmentService;
import com.fpoly.marcusstore.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/staff/order-assignments")
@PreAuthorize("hasRole('STAFF') and hasAuthority('ORDER_UPDATE')")
@RequiredArgsConstructor
public class StaffOrderAssignmentController {
    private final OrderAssignmentService assignmentService;
    private final OrderService orderService;

    @GetMapping("/status")
    public StaffAssignmentStatusResponse getStatus() {
        return assignmentService.getCurrentStaffStatus();
    }

    @PutMapping("/availability")
    public StaffAssignmentStatusResponse setAvailability(@Valid @RequestBody StaffAvailabilityRequest request) {
        return assignmentService.setCurrentStaffAvailability(request.getAcceptingOrders());
    }

    @PostMapping("/claim-next")
    public OrderDetailResponse claimNext() {
        return orderService.getOrderDetailResponse(assignmentService.claimNextOrder());
    }
}
