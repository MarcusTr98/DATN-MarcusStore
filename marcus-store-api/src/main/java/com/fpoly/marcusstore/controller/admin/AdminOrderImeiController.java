package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.UpdateOrderImeiRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderImeiAssignmentResponse;
import com.fpoly.marcusstore.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders/{orderCode}/imeis")
@PreAuthorize("hasAuthority('ORDER_UPDATE')")
@RequiredArgsConstructor
@Validated
public class AdminOrderImeiController {
    private final OrderService orderService;

    @GetMapping("/preview")
    public List<OrderImeiAssignmentResponse> getPreview(@PathVariable @Size(min = 1, max = 50) String orderCode) {
        return orderService.getImeiPreview(orderCode);
    }

    @PutMapping
    public OrderDetailResponse assignImeis(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody List<UpdateOrderImeiRequest> requests) {
        return orderService.assignOrderImeis(orderCode, requests);
    }

    @PostMapping("/processing")
    public OrderDetailResponse startProcessing(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody List<UpdateOrderImeiRequest> requests) {
        return orderService.startProcessingWithImei(orderCode, requests);
    }
}
