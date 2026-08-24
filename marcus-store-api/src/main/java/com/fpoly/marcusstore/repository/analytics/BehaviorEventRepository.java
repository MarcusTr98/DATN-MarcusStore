package com.fpoly.marcusstore.repository.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BehaviorEventRepository {
    private final JdbcTemplate jdbcTemplate;

    public void insert(String eventType, String sessionId, Integer productId, Integer orderId) {
        jdbcTemplate.update("""
                INSERT INTO Customer_Behavior_Events(event_type, session_id, product_id, order_id)
                VALUES (?, ?, ?, ?)
                """, eventType, sessionId, productId, orderId);
    }

    public int deleteExpired(int retentionDays) {
        return jdbcTemplate.update("""
                DELETE FROM Customer_Behavior_Events
                WHERE created_at < DATEADD(DAY, -?, SYSDATETIME())
                """, retentionDays);
    }

    public String findSessionByOrderId(Integer orderId) {
        return jdbcTemplate.query("""
                SELECT TOP 1 session_id FROM Customer_Behavior_Events
                WHERE order_id = ? AND event_type = 'ORDER_CREATED'
                ORDER BY created_at DESC
                """, rs -> rs.next() ? rs.getString(1) : null, orderId);
    }

    public long[] funnel(LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.queryForObject(
                """
                        WITH SessionSteps AS (
                            SELECT session_id,
                                MIN(CASE WHEN event_type='PRODUCT_VIEW' THEN created_at END) AS viewed_at,
                                MIN(CASE WHEN event_type='CHECKOUT_STARTED' THEN created_at END) AS checkout_at,
                                MIN(CASE WHEN event_type='ORDER_CREATED' THEN created_at END) AS ordered_at,
                                MIN(CASE WHEN event_type='PAYMENT_SUCCESS' THEN created_at END) AS paid_at,
                                MIN(CASE WHEN event_type='AI_QUESTION' THEN created_at END) AS asked_at,
                                MIN(CASE WHEN event_type='AI_PRODUCT_CLICK' THEN created_at END) AS ai_clicked_at
                            FROM Customer_Behavior_Events
                            WHERE created_at >= ? AND created_at < ? AND session_id IS NOT NULL
                            GROUP BY session_id
                        )
                        SELECT
                            COUNT_BIG(CASE WHEN viewed_at IS NOT NULL THEN 1 END),
                            COUNT_BIG(CASE WHEN viewed_at IS NOT NULL AND checkout_at >= viewed_at THEN 1 END),
                            COUNT_BIG(CASE WHEN viewed_at IS NOT NULL AND checkout_at >= viewed_at AND ordered_at >= checkout_at THEN 1 END),
                            COUNT_BIG(CASE WHEN viewed_at IS NOT NULL AND checkout_at >= viewed_at AND ordered_at >= checkout_at AND paid_at >= ordered_at THEN 1 END),
                            COUNT_BIG(CASE WHEN asked_at IS NOT NULL THEN 1 END),
                            COUNT_BIG(CASE WHEN asked_at IS NOT NULL AND ai_clicked_at >= asked_at THEN 1 END)
                        FROM SessionSteps
                        """,
                (rs, row) -> new long[] {
                        rs.getLong(1), rs.getLong(2), rs.getLong(3),
                        rs.getLong(4), rs.getLong(5), rs.getLong(6)
                }, from, to);
    }

    public List<TopAiProductClick> findTopAiProductClicks() {
        return jdbcTemplate.query("""
                SELECT TOP 10 e.product_id, p.product_name, COUNT_BIG(*)
                FROM Customer_Behavior_Events e
                INNER JOIN Products p ON p.product_id = e.product_id
                WHERE e.event_type = 'AI_PRODUCT_CLICK' AND e.product_id IS NOT NULL
                GROUP BY e.product_id, p.product_name
                ORDER BY COUNT_BIG(*) DESC
                """, (rs, row) -> new TopAiProductClick(rs.getInt(1), rs.getString(2), rs.getLong(3)));
    }

    public record TopAiProductClick(Integer productId, String productName, Long clickCount) {}
}
