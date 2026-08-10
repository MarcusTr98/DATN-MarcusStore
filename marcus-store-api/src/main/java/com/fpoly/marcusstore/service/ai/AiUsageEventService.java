package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiUsageSummaryResponse;
import com.fpoly.marcusstore.dto.ai.AiSalesFunnelResponse;
import com.fpoly.marcusstore.repository.analytics.AiUsageEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiUsageEventService {

    private static final int MAX_RESPONSE_TIME_MS = 120_000;
    private final AiUsageEventRepository repository;

    // Marcus thêm: chỉ lưu telemetry tối thiểu; tuyệt đối không nhận câu hỏi,
    // câu trả lời, IP, userId hoặc dữ liệu nhận dạng khách hàng.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordChatResult(String sessionId, String adviceId, boolean providerSuccessful, long responseTimeMs) {
        save(sessionId, adviceId, providerSuccessful ? "CHAT_RESPONSE" : "CHAT_FAILED", null, responseTimeMs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordProductClick(String sessionId, String adviceId, Integer productId) {
        if (!repository.existsChatResponse(sessionId, adviceId)) return;
        save(sessionId, adviceId, "PRODUCT_CLICK", productId, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFeedback(String sessionId, String adviceId, boolean helpful) {
        if (!repository.existsChatResponse(sessionId, adviceId)) {
            throw new IllegalArgumentException("Câu tư vấn không thuộc phiên hiện tại.");
        }
        save(sessionId, adviceId, helpful ? "FEEDBACK_HELPFUL" : "FEEDBACK_NOT_HELPFUL", null, null);
    }

    private void save(String sessionId, String adviceId, String eventType, Integer productId, Long responseTimeMs) {
        if (sessionId == null || !sessionId.matches(
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")) {
            return;
        }
        Integer safeResponseTime = null;
        if (responseTimeMs != null) {
            safeResponseTime = (int) Math.min(Math.max(responseTimeMs, 0), MAX_RESPONSE_TIME_MS);
        }
        repository.insert(sessionId, adviceId, eventType, productId, safeResponseTime);
    }

    @Transactional(readOnly = true)
    public AiUsageSummaryResponse summarize(LocalDate fromDate, LocalDate toDate) {
        LocalDate safeTo = toDate == null ? LocalDate.now() : toDate;
        LocalDate safeFrom = fromDate == null ? safeTo.minusDays(29) : fromDate;
        if (safeFrom.isAfter(safeTo) || safeFrom.isBefore(safeTo.minusYears(2))) {
            throw new IllegalArgumentException("Khoảng ngày thống kê AI không hợp lệ.");
        }

        var row = repository.summarize(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay());
        long success = row.successfulChats();
        long failed = row.failedChats();
        long clicks = row.productClicks();
        // Marcus sửa: lượt tư vấn thành công = phiên có feedback Hữu ích hoặc click
        // sản phẩm; mẫu số là toàn bộ phiên đã nhận phản hồi/fallback.
        long chats = row.totalAdvisorSessions();
        double successRate = chats == 0 ? 0 : round(success * 100.0 / chats);
        double clickRate = success == 0 ? 0 : round(clicks * 100.0 / success);
        return new AiUsageSummaryResponse(
                success,
                failed,
                clicks,
                row.uniqueSessions(),
                successRate,
                clickRate,
                row.averageResponseTimeMs());
    }

    @Transactional(readOnly = true)
    public AiSalesFunnelResponse salesFunnel(LocalDate fromDate, LocalDate toDate) {
        LocalDate safeTo = toDate == null ? LocalDate.now() : toDate;
        LocalDate safeFrom = fromDate == null ? safeTo.minusDays(29) : fromDate;
        if (safeFrom.isAfter(safeTo) || safeFrom.isBefore(safeTo.minusYears(2))) {
            throw new IllegalArgumentException("Khoảng ngày funnel AI không hợp lệ.");
        }
        var row = repository.salesFunnel(
                safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay());
        return new AiSalesFunnelResponse(
                row.questions(), row.responses(), row.helpful(), row.clicks(),
                row.checkouts(), row.orders(), row.paid(),
                rate(row.responses(), row.questions()),
                rate(row.helpful(), row.responses()),
                rate(row.clicks(), row.questions()),
                rate(row.checkouts(), row.clicks()),
                rate(row.orders(), row.checkouts()),
                rate(row.paid(), row.orders()));
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double rate(long value, long base) {
        return base == 0 ? 0 : round(value * 100.0 / base);
    }
}
