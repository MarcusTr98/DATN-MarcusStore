package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.OrderTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

    private final OrderRepository orderRepository;
    private final OrderTransactionService transactionService;

    @Value("${ghn.webhook.token:}")
    private String ghnWebhookToken;

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
            @RequestHeader(value = "X-Verification-Token", required = false) String verifyToken,
            @RequestBody Map<String, Object> payload) {

        log.info("[GHN Webhook] Nhận payload: {}", payload);

        if (ghnWebhookToken != null && !ghnWebhookToken.isBlank() && !ghnWebhookToken.equals(verifyToken)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // Tinh chỉnh: trim() để đảm bảo khớp dữ liệu trong DB
        String trackingCode = (String) payload.get("order_code");
        String ghnStatus = (String) payload.get("status");

        if (trackingCode == null || ghnStatus == null)
            return ResponseEntity.badRequest().body("Missing data");
        trackingCode = trackingCode.trim();

        Order order = orderRepository.findByTrackingCode(trackingCode).orElse(null);
        if (order == null) {
            log.warn("❌ [GHN Webhook] Không tìm thấy đơn với tracking_code: '{}'", trackingCode);
            return ResponseEntity.ok("Order not found, ignored");
        }

        String newStatus = GHN_STATUS_MAP.get(ghnStatus.toLowerCase());
        if (newStatus == null || newStatus.equals(order.getOrderStatus())) {
            return ResponseEntity.ok("No status change needed");
        }

        log.info("🚀 [GHN Webhook] Cập nhật đơn {} từ {} -> {}", order.getOrderCode(), order.getOrderStatus(),
                newStatus);

        order.setOrderStatus(newStatus);

        // Xử lý Transaction (Đối soát tài chính tự động)
        if ("COMPLETED".equals(newStatus)) {
            order.setPaymentStatus("PAID");
            transactionService.recordTransaction(order, order.getFinalAmount(), "COD_COLLECTION", "SUCCESS",
                    "GHN báo hoàn tất, xác nhận thu tiền.");
        } else if ("FAILED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            // Trường hợp này nếu là VNPAY thì bạn nên mở rộng thêm logic Refund nếu cần
            transactionService.recordTransaction(order, BigDecimal.ZERO, "REFUND_PENDING", "PENDING",
                    "Đơn hàng hoàn/hủy, chờ đối soát.");
        }

        orderRepository.save(order);
        return ResponseEntity.ok("OK");
    }
}