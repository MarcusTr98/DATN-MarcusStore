package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateRefundRequest;
import com.fpoly.marcusstore.dto.response.RefundResponse;
import com.fpoly.marcusstore.service.RefundProcessor;
import com.fpoly.marcusstore.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ORDER_VIEW')")
@RequiredArgsConstructor
@Validated
public class AdminRefundController {
    private final RefundService refundService;
    private final RefundProcessor refundProcessor;

    @GetMapping("/refunds")
    public Page<RefundResponse> getRefunds(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return refundService.getRefunds(status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
    }

    @GetMapping("/orders/{orderCode}/refund")
    public ResponseEntity<RefundResponse> getOrderRefund(@PathVariable @Size(min = 1, max = 50) String orderCode) {
        RefundResponse refund = refundService.getLatestRefundForOrder(orderCode);
        return refund == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(refund);
    }

    @PostMapping("/orders/{orderCode}/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public RefundResponse createRefund(@PathVariable @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody CreateRefundRequest request) {
        return refundService.requestManualRefund(orderCode, request.getReason());
    }

    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public RefundResponse approve(@PathVariable @Positive Long refundId) {
        return refundProcessor.approve(refundId);
    }

    @PostMapping("/refunds/{refundId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public RefundResponse retry(@PathVariable @Positive Long refundId) {
        return refundProcessor.retry(refundId);
    }

    @PostMapping("/refunds/{refundId}/reconcile")
    @PreAuthorize("hasRole('ADMIN')")
    public RefundResponse reconcile(@PathVariable @Positive Long refundId) {
        return refundProcessor.reconcile(refundId);
    }

    @PostMapping("/refunds/{refundId}/sandbox-confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public RefundResponse confirmSandbox(@PathVariable @Positive Long refundId,
            @Valid @RequestBody CreateRefundRequest request) {
        return refundProcessor.confirmSandbox(refundId, request.getReason());
    }
}
