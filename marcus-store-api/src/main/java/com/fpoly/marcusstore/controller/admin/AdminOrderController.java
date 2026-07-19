package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateRefundRequest;
import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.OrderStatsResponse;
import com.fpoly.marcusstore.dto.response.RefundResponse;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.RefundProcessor;
import com.fpoly.marcusstore.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ORDER_VIEW')")
@RequiredArgsConstructor
public class AdminOrderController {
    private final OrderService orderService;
    private final RefundService refundService;
    private final RefundProcessor refundProcessor;

    @GetMapping("/orders")
    public Page<OrderResponse> getAllOrder(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return orderService.getOrdersPage(keyword, paymentMethod, orderStatus, fromDate, toDate, pageable);
    }

    @GetMapping("/orders/stats")
    public OrderStatsResponse getOrderStats(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String orderStatus) {
        return orderService.getOrderStats(keyword, paymentMethod, orderStatus);
    }

    @GetMapping("/orders/filter-options")
    public Map<String, Object> getOrderFilterOptions() {
        return Map.of(
                "paymentMethods", orderService.getPaymentMethods(),
                "orderStatuses", orderService.getOrderStatuses());
    }

    @GetMapping("/order/{orderCode}")
    public OrderDetailResponse getDetailResponse(@PathVariable("orderCode") String orderCode) {
        return orderService.getOrderDetailResponse(orderCode);
    }

    @PutMapping("/order/{orderCode}")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public OrderDetailResponse updateStatusOrder(@PathVariable("orderCode") String orderCode,
            @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatusOrder(orderCode, request);
    }

    // Marcus lam refund
    // Marcus thêm nhóm API quản trị refund: xem, tạo, duyệt và retry.
    @GetMapping("/refunds")
    public Page<RefundResponse> getRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return refundService.getRefunds(
                status,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
    }

    @GetMapping("/orders/{orderCode}/refund")
    public ResponseEntity<RefundResponse> getOrderRefund(@PathVariable String orderCode) {
        RefundResponse refund = refundService.getLatestRefundForOrder(orderCode);
        return refund == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(refund);
    }

    @PostMapping("/orders/{orderCode}/refunds")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public RefundResponse createRefund(
            @PathVariable String orderCode,
            @Valid @RequestBody CreateRefundRequest request) {
        return refundService.requestManualRefund(orderCode, request.getReason());
    }

    @PostMapping("/refunds/{refundId}/approve")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public RefundResponse approveRefund(@PathVariable Long refundId) {
        return refundProcessor.approve(refundId);
    }

    @PostMapping("/refunds/{refundId}/retry")
    @PreAuthorize("hasAuthority('ORDER_UPDATE')")
    public RefundResponse retryRefund(@PathVariable Long refundId) {
        return refundProcessor.retry(refundId);
    }
}
