package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.OrderDetailResponse;
import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.dto.response.ClientRefundResponse;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class UserOrderController {
    private final OrderService orderService;
    private final RefundService refundService;

    @GetMapping
    public List<OrderResponse> getAllOrder() {
        return orderService.getUserOrder();
    }

    @GetMapping("/{orderCode}")
    public OrderDetailResponse getUserOrderDetail(@PathVariable("orderCode") String orderCode) {
        return orderService.getUserOrderDetail(orderCode);
    }

    // Marcus thêm endpoint để khách theo dõi refund của chính đơn hàng của mình.
    @GetMapping("/{orderCode}/refund")
    public ResponseEntity<ClientRefundResponse> getUserRefund(@PathVariable String orderCode) {
        ClientRefundResponse refund = refundService.getCurrentUserRefund(orderCode);
        return refund == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(refund);
    }

    @PostMapping("/{orderCode}/cancel")
    public OrderDetailResponse cancelUserOrder(
            @PathVariable("orderCode") @Size(min = 1, max = 50) String orderCode,
            @Valid @RequestBody(required = false) UpdateOrderStatusRequest request) {
        String reason = request == null ? null : request.getNote();
        String reasonCode = request == null ? null : request.getCancellationReasonCode();
        return orderService.cancelUserOrder(orderCode, reasonCode, reason);
    }

    // Marcus thêm: khách chỉ xác nhận đã nhận trên chính đơn của mình; backend
    // quyết định trạng thái hợp lệ, frontend không được truyền status tùy ý.
    @PostMapping("/{orderCode}/confirm-received")
    public OrderDetailResponse confirmReceived(
            @PathVariable("orderCode") @Size(min = 1, max = 50) String orderCode) {
        return orderService.confirmUserReceivedOrder(orderCode);
    }
}
