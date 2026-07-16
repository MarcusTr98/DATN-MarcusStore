package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.core.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientSuggestedProductRepository extends JpaRepository<Product, Integer> {

    @EntityGraph(attributePaths = { "skus" })
    @Query("SELECT p FROM Product p " +
           "WHERE p.status = true " +
           "AND (p.category.categoryId = :categoryId OR p.category.parent.categoryId = :categoryId) " +
           "ORDER BY p.productId DESC")
    List<Product> findSuggestedByCategory(@Param("categoryId") Integer categoryId, Pageable pageable);

    interface RatingAggProjection {
        Integer getProductId();
        Double getAvgRating();
        Long getReviewCount();
    }

    @Query(value = """
                    SELECT
                        ce.product_id AS productId,
                        AVG(CAST(ce.rating AS FLOAT)) AS avgRating,
                        COUNT(ce.review_id) AS reviewCount
                    FROM Comments_Evaluations ce
                    WHERE ce.is_approved = 1
                      AND ce.product_id IN (:productIds)
                    GROUP BY ce.product_id
                    """, nativeQuery = true)
    List<RatingAggProjection> findRatingAggByProductIds(@Param("productIds") List<Integer> productIds);
}