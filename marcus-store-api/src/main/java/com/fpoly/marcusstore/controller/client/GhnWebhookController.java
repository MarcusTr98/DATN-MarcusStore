package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.service.GhnStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

    private final GhnStatusService ghnStatusService;

    @Value("${ghn.webhook.token:}")
    private String ghnWebhookToken;

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
            log.error("[GHN Webhook] Chưa cấu hình ghn.webhook.token, từ chối webhook để bảo vệ dữ liệu đơn hàng.");
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

        GhnStatusService.SyncResult result = ghnStatusService.applyStatus(
                trackingCode, ghnStatus, "WEBHOOK");

        return switch (result) {
            case UPDATED -> ResponseEntity.ok("OK");
            case NO_CHANGE -> ResponseEntity.ok("No status change needed");
            case INVALID_TRANSITION -> ResponseEntity.ok("Invalid transition, ignored");
            case UNSUPPORTED_STATUS -> ResponseEntity.ok("Unsupported status, ignored");
            case ORDER_NOT_FOUND -> ResponseEntity.ok("Order not found, ignored");
        };
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
