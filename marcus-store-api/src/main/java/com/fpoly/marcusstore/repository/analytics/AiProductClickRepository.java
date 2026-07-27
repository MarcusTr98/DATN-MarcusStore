package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.entity.analytics.AiProductClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AiProductClickRepository extends JpaRepository<AiProductClick, Long> {

    boolean existsBySessionIdAndProductIdAndClickedAtAfter(
            String sessionId, Integer productId, LocalDateTime clickedAt);

    @Query(value = """
            SELECT TOP 10
                c.product_id AS productId,
                p.product_name AS productName,
                COUNT_BIG(*) AS clickCount
            FROM AI_Product_Clicks c
            INNER JOIN Products p ON p.product_id = c.product_id
            GROUP BY c.product_id, p.product_name
            ORDER BY COUNT_BIG(*) DESC
            """, nativeQuery = true)
    List<AiProductClickStatProjection> findTopClickedProducts();

    interface AiProductClickStatProjection {
        Integer getProductId();

        String getProductName();

        Long getClickCount();
    }
}
