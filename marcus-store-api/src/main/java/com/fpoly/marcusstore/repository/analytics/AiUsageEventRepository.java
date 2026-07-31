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
            String eventType,
            Integer productId,
            Integer responseTimeMs) {
        jdbcTemplate.update("""
                INSERT INTO AI_Usage_Events
                    (session_id, event_type, product_id, response_time_ms)
                VALUES (?, ?, ?, ?)
                """, sessionId, eventType, productId, responseTimeMs);
    }

    public AiUsageSummaryRow summarize(LocalDateTime fromDate, LocalDateTime toDate) {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT_BIG(CASE WHEN event_type = 'CHAT_SUCCESS' THEN 1 END),
                    COUNT_BIG(CASE WHEN event_type = 'CHAT_FAILED' THEN 1 END),
                    COUNT_BIG(CASE WHEN event_type = 'PRODUCT_CLICK' THEN 1 END),
                    COUNT_BIG(DISTINCT session_id),
                    COALESCE(AVG(CASE
                        WHEN event_type = 'CHAT_SUCCESS' THEN CAST(response_time_ms AS BIGINT)
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
                        resultSet.getLong(5)),
                fromDate,
                toDate);
    }

    public record AiUsageSummaryRow(
            long successfulChats,
            long failedChats,
            long productClicks,
            long uniqueSessions,
            long averageResponseTimeMs) {
    }
}
