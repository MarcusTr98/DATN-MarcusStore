package com.fpoly.marcusstore.repository.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class AiUsageEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(
            String sessionId,
            String adviceId,
            String eventType,
            Integer productId,
            Integer responseTimeMs) {
        jdbcTemplate.update("""
                INSERT INTO AI_Usage_Events
                    (session_id, advice_id, event_type, product_id, response_time_ms)
                SELECT ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM AI_Usage_Events
                    WHERE advice_id = ? AND event_type IN ('FEEDBACK_HELPFUL', 'FEEDBACK_NOT_HELPFUL')
                ) OR ? NOT IN ('FEEDBACK_HELPFUL', 'FEEDBACK_NOT_HELPFUL')
                """, sessionId, adviceId, eventType, productId, responseTimeMs, adviceId, eventType);
    }

    public AiUsageSummaryRow summarize(LocalDateTime fromDate, LocalDateTime toDate) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT_BIG(DISTINCT CASE
                        WHEN event_type IN ('FEEDBACK_HELPFUL', 'PRODUCT_CLICK') THEN session_id
                    END),
                    COUNT_BIG(CASE WHEN event_type = 'CHAT_FAILED' THEN 1 END),
                    COUNT_BIG(CASE WHEN event_type = 'PRODUCT_CLICK' THEN 1 END),
                    COUNT_BIG(DISTINCT session_id),
                    COUNT_BIG(DISTINCT CASE
                        WHEN event_type IN ('CHAT_RESPONSE', 'CHAT_SUCCESS', 'CHAT_FAILED') THEN session_id
                    END),
                    COALESCE(AVG(CASE
                        WHEN event_type IN ('CHAT_RESPONSE', 'CHAT_SUCCESS') THEN CAST(response_time_ms AS BIGINT)
                    END), 0)
                FROM AI_Usage_Events
                WHERE created_at >= ?
                  AND created_at < ?
                """,
                (resultSet, rowNum) -> new AiUsageSummaryRow(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getLong(4),
                        resultSet.getLong(5),
                        resultSet.getLong(6)),
                fromDate,
                toDate);
    }

    public record AiUsageSummaryRow(
            long successfulChats,
            long failedChats,
            long productClicks,
            long uniqueSessions,
            long totalAdvisorSessions,
            long averageResponseTimeMs) {
    }
}
