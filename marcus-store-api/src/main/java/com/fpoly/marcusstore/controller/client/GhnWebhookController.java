package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.OrderTransactionService;
import com.fpoly.marcusstore.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

    private final OrderRepository orderRepository;
    private final OrderTransactionService transactionService;
    private final OrderPaymentService orderPaymentService;

    @Value("${ghn.webhook.token:}")
    private String ghnWebhookToken;

    private static final Map<String, String> GHN_STATUS_MAP = Map.of(
            "picking", "SHIPPING",
            "picked", "SHIPPING",
            "delivering", "SHIPPING",
            "delivered", "DELIVERED",
            "delivery_fail", "FAILED",
            "return", "FAILED",
            "cancel", "CANCELLED");

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Verification-Token", required = false) String verifyToken,
            Authentication authentication,
            @RequestBody Map<String, Object> payload) {

        boolean configuredWebhookToken = ghnWebhookToken != null && !ghnWebhookToken.isBlank();
        boolean validWebhookToken = configuredWebhookToken && isValidToken(verifyToken);
        boolean authorizedAdmin = hasOrderUpdatePermission(authentication);

        if (!configuredWebhookToken && !authorizedAdmin) {
            log.error("[GHN Webhook] Chưa cấu hình ghn.webhook.token; từ chối webhook để bảo vệ dữ liệu đơn hàng.");
            return ResponseEntity.status(503).body("Webhook verification is not configured");
        }

        if (!validWebhookToken && !authorizedAdmin) {
            log.warn("[GHN Webhook] Token xác thực không hợp lệ.");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        log.info("[GHN Webhook] Nhận payload đã xác thực: {}", payload);
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
        if (newStatus == null) {
            return ResponseEntity.ok("Unsupported status, ignored");
        }
        if (newStatus.equals(order.getOrderStatus())) {
            if ("DELIVERED".equals(newStatus)) {
                orderPaymentService.handleCodDelivered(
                        order,
                        "GHN_WEBHOOK_DELIVERED:" + trackingCode);
                orderRepository.save(order);
            }
            return ResponseEntity.ok("No status change needed");
        }

        log.info("🚀 [GHN Webhook] Cập nhật đơn {} từ {} -> {}", order.getOrderCode(), order.getOrderStatus(),
                newStatus);

        order.setOrderStatus(newStatus);

        // GHN dùng trạng thái delivered, không gửi COMPLETED. Chốt đúng transaction COD
        // đang chờ.
        if ("DELIVERED".equals(newStatus)) {
            orderPaymentService.handleCodDelivered(
                    order,
                    "GHN_WEBHOOK_DELIVERED:" + trackingCode);
        } else if ("FAILED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            // đợi mở rộng refun
            transactionService.recordTransaction(order, BigDecimal.ZERO, "REFUND_PENDING", "PENDING",
                    "Đơn hàng hoàn/hủy, chờ đối soát.");
        }

        orderRepository.save(order);
        return ResponseEntity.ok("OK");
    }

    private boolean isValidToken(String receivedToken) {
        if (receivedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                ghnWebhookToken.getBytes(StandardCharsets.UTF_8),
                receivedToken.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasOrderUpdatePermission(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .anyMatch(authority -> "SUPER_ADMIN".equals(authority)
                                || "ROLE_ADMIN".equals(authority)
                                || "ORDER_UPDATE".equals(authority));
    }
}
