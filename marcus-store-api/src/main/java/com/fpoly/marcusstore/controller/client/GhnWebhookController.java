package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    // FIX: verify token GHN để tránh giả mạo webhook
    // Lấy từ GHN Dashboard > Settings > Webhook > Token
    @Value("${ghn.webhook.token:}")
    private String ghnWebhookToken;

    // Map trạng thái GHN -> trạng thái nội bộ
    private static final Map<String, String> GHN_STATUS_MAP = Map.of(
            "picking", "SHIPPING",
            "picked", "SHIPPING",
            "delivering", "SHIPPING",
            "delivered", "COMPLETED",
            "delivery_fail", "FAILED",
            "return", "FAILED",
            "cancel", "CANCELLED");

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            // FIX: GHN gửi token qua header để xác thực
            @RequestHeader(value = "X-Verification-Token", required = false) String verifyToken,
            @RequestBody Map<String, Object> payload) {

        log.info("[GHN Webhook] Nhận payload: {}", payload);

        // Verify token nếu đã cấu hình
        if (ghnWebhookToken != null && !ghnWebhookToken.isBlank()) {
            if (!ghnWebhookToken.equals(verifyToken)) {
                log.warn("[GHN Webhook] Token không hợp lệ, bỏ qua");
                return ResponseEntity.status(401).body("Unauthorized");
            }
        }

        String trackingCode = (String) payload.get("order_code");
        String ghnStatus = (String) payload.get("status");

        if (trackingCode == null || ghnStatus == null) {
            log.warn("[GHN Webhook] Thiếu order_code hoặc status");
            return ResponseEntity.badRequest().body("Missing order_code or status");
        }

        Order order = orderRepository.findByTrackingCode(trackingCode).orElse(null);
        if (order == null) {
            log.warn("[GHN Webhook] Không tìm thấy đơn với tracking_code={}", trackingCode);
            return ResponseEntity.ok("Order not found, ignored");
        }

        String newStatus = GHN_STATUS_MAP.get(ghnStatus.toLowerCase());
        if (newStatus == null) {
            log.info("[GHN Webhook] Status GHN '{}' không cần xử lý", ghnStatus);
            return ResponseEntity.ok("Status ignored");
        }

        // Tránh ghi đè nếu đã ở trạng thái cuối
        if ("COMPLETED".equals(order.getOrderStatus()) || "CANCELLED".equals(order.getOrderStatus())) {
            log.info("[GHN Webhook] Đơn {} đã ở trạng thái cuối, bỏ qua", order.getOrderCode());
            return ResponseEntity.ok("Already finalized");
        }

        log.info("[GHN Webhook] Cập nhật đơn {} từ {} -> {}",
                order.getOrderCode(), order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            order.setPaymentStatus("PAID"); // COD giao thành công => xem như đã thu tiền
        }
        orderRepository.save(order);

        // Ghi lịch sử (created_by = null vì hệ thống tự cập nhật)
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(newStatus);
        history.setTitle(getTitle(newStatus));
        history.setNote("Cập nhật tự động từ GHN. Trạng thái GHN: " + ghnStatus);
        orderStatusHistoryRepository.save(history);

        return ResponseEntity.ok("OK");
    }

    private String getTitle(String status) {
        return switch (status) {
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "COMPLETED" -> "Giao hàng thành công";
            case "FAILED" -> "Giao hàng không thành công";
            case "CANCELLED" -> "Đơn hàng đã hủy";
            default -> "Cập nhật trạng thái";
        };
    }
}