package com.fpoly.marcusstore.repository.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class AiUsageEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsChatResponse(String sessionId, String adviceId) {
        if (sessionId == null || adviceId == null) return false;
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT_BIG(*) FROM AI_Usage_Events
                WHERE session_id = ? AND advice_id = ?
                  AND event_type IN ('CHAT_RESPONSE', 'CHAT_SUCCESS')
                """, Long.class, sessionId, adviceId);
        return count != null && count > 0;
    }

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

    public AiSalesFunnelRow salesFunnel(LocalDateTime fromDate, LocalDateTime toDate) {
        return jdbcTemplate.queryForObject("""
                WITH AiJourneys AS (
                    SELECT session_id, MIN(created_at) AS asked_at
                    FROM Customer_Behavior_Events
                    WHERE event_type = 'AI_QUESTION'
                      AND session_id IS NOT NULL
                      AND created_at >= ? AND created_at < ?
                    GROUP BY session_id
                )
                SELECT
                    COUNT_BIG(*),
                    COALESCE(SUM(CAST(COALESCE(response_event.hit, 0) AS BIGINT)), 0),
                    COALESCE(SUM(CAST(COALESCE(helpful_event.hit, 0) AS BIGINT)), 0),
                    COALESCE(SUM(CAST(COALESCE(click_event.hit, 0) AS BIGINT)), 0),
                    COALESCE(SUM(CAST(COALESCE(checkout_event.hit, 0) AS BIGINT)), 0),
                    COALESCE(SUM(CAST(COALESCE(order_event.hit, 0) AS BIGINT)), 0),
                    COALESCE(SUM(CAST(COALESCE(payment_event.hit, 0) AS BIGINT)), 0)
                FROM AiJourneys j
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM AI_Usage_Events u
                        WHERE u.session_id = j.session_id
                          AND u.event_type IN ('CHAT_RESPONSE','CHAT_SUCCESS')
                          AND u.created_at >= j.asked_at AND u.created_at < ?
                ) response_event
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM AI_Usage_Events u
                        WHERE u.session_id = j.session_id
                          AND u.event_type = 'FEEDBACK_HELPFUL'
                          AND u.created_at >= j.asked_at AND u.created_at < ?
                ) helpful_event
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM Customer_Behavior_Events e
                        WHERE e.session_id = j.session_id AND e.event_type = 'AI_PRODUCT_CLICK'
                          AND e.created_at >= j.asked_at AND e.created_at < ?
                ) click_event
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM Customer_Behavior_Events e
                        WHERE e.session_id = j.session_id AND e.event_type = 'CHECKOUT_STARTED'
                          AND e.created_at >= j.asked_at AND e.created_at < ?
                ) checkout_event
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM Customer_Behavior_Events e
                        WHERE e.session_id = j.session_id AND e.event_type = 'ORDER_CREATED'
                          AND e.created_at >= j.asked_at AND e.created_at < ?
                ) order_event
                OUTER APPLY (
                    SELECT TOP 1 1 AS hit FROM Customer_Behavior_Events e
                        WHERE e.session_id = j.session_id AND e.event_type = 'PAYMENT_SUCCESS'
                          AND e.created_at >= j.asked_at AND e.created_at < ?
                ) payment_event
                """, (rs, rowNum) -> new AiSalesFunnelRow(
                        rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                        rs.getLong(5), rs.getLong(6), rs.getLong(7)),
                fromDate, toDate, toDate, toDate, toDate, toDate, toDate, toDate);
    }

    public record AiUsageSummaryRow(
            long successfulChats,
            long failedChats,
            long productClicks,
            long uniqueSessions,
            long totalAdvisorSessions,
            long averageResponseTimeMs) {
    }

    public record AiSalesFunnelRow(
            long questions,
            long responses,
            long helpful,
            long clicks,
            long checkouts,
            long orders,
            long paid) {
    }
}
