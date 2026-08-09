package com.fpoly.marcusstore.controller.publicapi;

import com.fpoly.marcusstore.dto.ai.AiAdvisorRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import com.fpoly.marcusstore.dto.ai.AiProductClickRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorFeedbackRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.ai.AiAdvisorService;
import com.fpoly.marcusstore.service.ai.AiProductClickService;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import com.fpoly.marcusstore.service.analytics.BehaviorEventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping("/api/public/ai-advisor")
@RequiredArgsConstructor
public class AiAdvisorController {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private final AiAdvisorService aiAdvisorService;
    private final AiProductClickService aiProductClickService;
    private final AiUsageEventService usageEventService;
    private final BehaviorEventService behaviorEventService;
    private final Map<String, Deque<Instant>> requestWindows = new ConcurrentHashMap<>();

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiAdvisorResponse>> chat(
            @Valid @RequestBody AiAdvisorRequest request, HttpServletRequest servletRequest) {
        enforceRateLimit(clientKey(servletRequest));
        recordBehavior("AI_QUESTION", request.getSessionId(), null);
        long startedAt = System.nanoTime();
        try {
            AiAdvisorResponse response = aiAdvisorService.advise(request);
            prepareResponse(response);
            recordChatUsage(request.getSessionId(), response.getAdviceId(), !response.isFallbackUsed(), startedAt);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException exception) {
            recordChatUsage(request.getSessionId(), null, false, startedAt);
            throw exception;
        }
    }

    @PostMapping(value = "/chat-stream", produces = TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Valid @RequestBody AiAdvisorRequest request, HttpServletRequest servletRequest) {
        enforceRateLimit(clientKey(servletRequest));
        recordBehavior("AI_QUESTION", request.getSessionId(), null);
        // Marcus sửa: emitter phải sống lâu hơn timeout gọi Gemini để không đóng
        // kết nối khi nhà cung cấp vẫn đang tạo câu trả lời.
        SseEmitter emitter = new SseEmitter(75_000L);
        long startedAt = System.nanoTime();

        CompletableFuture.runAsync(() -> {
            try {
                AiAdvisorResponse response = aiAdvisorService.advise(request);
                prepareResponse(response);
                for (String token : response.getAnswer().split("(?<=\\s)")) {
                    emitter.send(SseEmitter.event().name("token").data(Map.of("token", token)));
                }
                emitter.send(SseEmitter.event().name("done").data(response));
                emitter.complete();
                recordChatUsage(request.getSessionId(), response.getAdviceId(), !response.isFallbackUsed(), startedAt);
            } catch (Exception exception) {
                recordChatUsage(request.getSessionId(), null, false, startedAt);
                sendSafeStreamError(emitter, exception);
            }
        });
        return emitter;
    }

    private void sendSafeStreamError(SseEmitter emitter, Exception exception) {
        try {
            // Marcus sửa: chỉ gửi lỗi nghiệp vụ đã kiểm soát; không đẩy exception
            // SQL/network nội bộ qua luồng SSE.
            String message = exception instanceof IllegalStateException && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Marcus AI đang gián đoạn. Vui lòng thử lại sau.";
            emitter.send(SseEmitter.event().name("advisor-error").data(Map.of("message", message)));
            emitter.complete();
        } catch (IOException ignored) {
            emitter.completeWithError(exception);
        }
    }

    @PostMapping("/product-click")
    public ResponseEntity<ApiResponse<String>> trackProductClick(
            @Valid @RequestBody AiProductClickRequest request) {
        aiProductClickService.track(request);
        recordProductClickUsage(request);
        recordBehavior("AI_PRODUCT_CLICK", request.getSessionId(), request.getProductId());
        return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận."));
    }

    private void recordBehavior(String type, String sessionId, Integer productId) {
        try {
            behaviorEventService.recordAi(type, sessionId, productId);
        } catch (RuntimeException ignored) {
        }
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<String>> feedback(
            @Valid @RequestBody AiAdvisorFeedbackRequest request) {
        usageEventService.recordFeedback(
                request.getSessionId(), request.getAdviceId(), request.getHelpful());
        return ResponseEntity.ok(ApiResponse.success("Cảm ơn bạn đã đánh giá."));
    }

    private void recordChatUsage(String sessionId, String adviceId, boolean providerSuccessful, long startedAt) {
        try {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            usageEventService.recordChatResult(sessionId, adviceId, providerSuccessful, elapsedMs);
        } catch (RuntimeException ignored) {
            // Marcus sửa: lỗi telemetry không được làm gián đoạn câu trả lời AI.
        }
    }

    private void prepareResponse(AiAdvisorResponse response) {
        if (response.getAdviceId() == null)
            response.setAdviceId(UUID.randomUUID().toString());
        if (response.getSource() == null)
            response.setSource("RULE");
    }

    private void recordProductClickUsage(AiProductClickRequest request) {
        try {
            usageEventService.recordProductClick(request.getSessionId(), request.getProductId());
        } catch (RuntimeException ignored) {
            // Marcus sửa: thống kê lỗi không được chặn khách mở trang sản phẩm.
        }
    }

    // Marcus thêm: giới hạn MVP theo IP để tránh một trình duyệt làm cạn hạn mức
    // AI.
    private void enforceRateLimit(String clientKey) {
        Deque<Instant> timestamps = requestWindows.computeIfAbsent(clientKey, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            Instant cutoff = Instant.now().minusSeconds(60);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn đang gửi quá nhanh. Vui lòng chờ một phút rồi thử lại.");
            }
            timestamps.addLast(Instant.now());
        }
    }

    private String clientKey(HttpServletRequest request) {
        // Marcus sửa: không tin X-Forwarded-For do client tự gửi để lách rate limit.
        return request.getRemoteAddr();
    }

    // Marcus thêm: dọn các IP không còn gửi request để bộ rate limit không giữ RAM
    // vô hạn.
    @Scheduled(fixedDelay = 600_000)
    public void cleanupRateLimitWindows() {
        Instant cutoff = Instant.now().minusSeconds(60);
        requestWindows.entrySet().removeIf(entry -> {
            Deque<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                    timestamps.removeFirst();
                }
                return timestamps.isEmpty();
            }
        });
    }
}
