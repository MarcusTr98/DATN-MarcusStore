package com.fpoly.marcusstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Marcus thêm: giới hạn các API công khai/tốn tài nguyên thuộc phạm vi Marcus.
 * Đây là lớp bảo vệ theo cửa sổ thời gian trong một instance, phù hợp môi
 * trường
 * DATN. Khi chạy nhiều instance có thể thay bộ đếm bằng Redis mà không đổi
 * controller.
 */
@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("POST:/api/auth/login", new Rule(10, Duration.ofMinutes(1))),
            Map.entry("POST:/api/auth/forgot", new Rule(5, Duration.ofMinutes(10))),
            Map.entry("POST:/api/auth/verify-otp", new Rule(8, Duration.ofMinutes(10))),
            Map.entry("POST:/api/auth/register/request", new Rule(5, Duration.ofMinutes(10))),
            Map.entry("POST:/api/auth/register/verify", new Rule(8, Duration.ofMinutes(10))),
            Map.entry("POST:/api/public/contact", new Rule(5, Duration.ofMinutes(10))),
            // Marcus thêm: event công khai chỉ phục vụ funnel, giới hạn đủ rộng cho
            // thao tác thật nhưng chặn script làm phình và sai lệch dữ liệu Analytics.
            Map.entry("POST:/api/public/behavior/events", new Rule(60, Duration.ofMinutes(1))),
            Map.entry("POST:/api/public/ai-advisor/chat", new Rule(12, Duration.ofMinutes(1))),
            Map.entry("POST:/api/public/ai-advisor/chat-stream", new Rule(12, Duration.ofMinutes(1))),
            Map.entry("POST:/api/admin/analytics/ai-report", new Rule(3, Duration.ofMinutes(1))));

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !RULES.containsKey(request.getMethod() + ":" + request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String route = request.getMethod() + ":" + request.getRequestURI();
        Rule rule = RULES.get(route);
        Instant now = Instant.now();
        String bucketKey = route + ":" + resolveRequester(request);

        Window window = windows.compute(bucketKey, (key, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new Window(1, now.plus(rule.duration()));
            }
            return new Window(current.count() + 1, current.expiresAt());
        });

        if (window.count() > rule.limit()) {
            long retryAfter = Math.max(1, Duration.between(now, window.expiresAt()).toSeconds());
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(
                    "{\"code\":429,\"message\":\"Bạn thao tác quá nhanh. Vui lòng thử lại sau.\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
        cleanupExpiredWindows(now);
    }

    private String resolveRequester(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }
        // Marcus sửa: không tin X-Forwarded-For do client tự gửi nếu chưa cấu hình
        // trusted proxy; remote address an toàn hơn cho môi trường demo/ngrok.
        return "ip:" + request.getRemoteAddr();
    }

    private void cleanupExpiredWindows(Instant now) {
        if (windows.size() > 2_000) {
            windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        }
    }

    private record Rule(int limit, Duration duration) {
    }

    private record Window(int count, Instant expiresAt) {
    }
}
