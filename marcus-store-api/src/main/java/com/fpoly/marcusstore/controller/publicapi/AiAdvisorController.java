package com.fpoly.marcusstore.controller.publicapi;

import com.fpoly.marcusstore.dto.ai.AiAdvisorRequest;
import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.ai.AiAdvisorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/public/ai-advisor")
@RequiredArgsConstructor
public class AiAdvisorController {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private final AiAdvisorService aiAdvisorService;
    private final Map<String, Deque<Instant>> requestWindows = new ConcurrentHashMap<>();

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<AiAdvisorResponse>> chat(
            @Valid @RequestBody AiAdvisorRequest request, HttpServletRequest servletRequest) {
        enforceRateLimit(clientKey(servletRequest));
        return ResponseEntity.ok(ApiResponse.success(aiAdvisorService.advise(request)));
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
                throw new IllegalStateException("Bạn đang gửi quá nhanh. Vui lòng chờ một phút rồi thử lại.");
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
