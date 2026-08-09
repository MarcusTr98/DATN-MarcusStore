package com.fpoly.marcusstore.repository.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

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
        return jdbcTemplate.queryForObject("""
                SELECT
                  COUNT_BIG(DISTINCT CASE WHEN event_type='PRODUCT_VIEW' THEN session_id END),
                  COUNT_BIG(DISTINCT CASE WHEN event_type='CHECKOUT_STARTED' THEN session_id END),
                  COUNT_BIG(DISTINCT CASE WHEN event_type='ORDER_CREATED' THEN session_id END),
                  COUNT_BIG(DISTINCT CASE WHEN event_type='PAYMENT_SUCCESS' THEN session_id END),
                  COUNT_BIG(DISTINCT CASE WHEN event_type='AI_QUESTION' THEN session_id END),
                  COUNT_BIG(DISTINCT CASE WHEN event_type='AI_PRODUCT_CLICK' THEN session_id END)
                FROM Customer_Behavior_Events
                WHERE created_at >= ? AND created_at < ?
                """, (rs, row) -> new long[] {
                rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), rs.getLong(6)
        }, from, to);
    }
}
