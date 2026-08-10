package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.service.GhnStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import com.fpoly.marcusstore.dto.request.GhnWebhookRequest;
import jakarta.validation.Valid;

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
            @Valid @RequestBody GhnWebhookRequest payload) {

        boolean configuredWebhookToken = ghnWebhookToken != null && !ghnWebhookToken.isBlank();
        boolean validWebhookToken = configuredWebhookToken && isValidToken(verifyToken);

        if (!configuredWebhookToken) {
            log.error("[GHN Webhook] Chưa cấu hình ghn.webhook.token, từ chối webhook để bảo vệ dữ liệu đơn hàng.");
            return ResponseEntity.status(503).body("Webhook verification is not configured");
        }

        // Marcus sửa: webhook là kênh máy-máy nên chỉ chấp nhận token nhà vận
        // chuyển. JWT/quyền Admin không được dùng để giả lập callback GHN.
        if (!validWebhookToken) {
            log.warn("[GHN Webhook] Token xác thực không hợp lệ.");
            return ResponseEntity.status(401).body("Unauthorized");
        }

        // Marcus sửa: không log toàn bộ payload provider và không cast Map tùy ý.
        String trackingCode = payload.getOrderCode().trim();
        String ghnStatus = payload.getStatus();

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

}
