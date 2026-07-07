package com.fpoly.marcusstore.repository.shopping;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;

@Repository
public interface ClientProductDetailRepository extends JpaRepository<Product, Integer>{

    
        @EntityGraph(attributePaths = { "category", "category.parent" })
        Optional<Product> findBySlugAndStatusTrue(String slug);

        // 2. Chỉ load images (1 List)
        @Query("SELECT p FROM Product p JOIN FETCH p.images WHERE p.slug = :slug AND p.status = true")
        Optional<Product> findBySlugWithImages(@Param("slug") String slug);

        // 3. Chỉ load skus (1 List)
        @Query("SELECT p FROM Product p " +
                        "LEFT JOIN FETCH p.skus s " +
                        "WHERE p.slug = :slug AND p.status = true")
        Optional<Product> findBySlugWithSkus(@Param("slug") String slug);

        //4
        @Query("SELECT DISTINCT s FROM ProductSku s " +
                        "LEFT JOIN FETCH s.attributeValues av " +
                        "LEFT JOIN FETCH av.attribute " +
                        "WHERE s.product.slug = :slug")
        List<ProductSku> findSkuAttributeValuesByProductSlug(@Param("slug") String slug);

        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.specValues psv LEFT JOIN FETCH psv.specAttribute WHERE p.slug = :slug AND p.status = true")
        Optional<Product> findBySlugWithSpecValues(@Param("slug") String slug);

        @Query(value = """
                        SELECT
                            AVG(CAST(ce.rating AS FLOAT)) AS avgRating,
                            COUNT(ce.review_id) AS reviewCount
                        FROM Comments_Evaluations ce
                        WHERE ce.is_approved = 1
                          AND ce.product_id = :productId
                        """, nativeQuery = true)
        RatingSummaryProjection findRatingSummaryByProductId(@Param("productId") Integer productId);

        @Query(value = """
                        SELECT
                            ce.rating AS star,
                            COUNT(ce.review_id) AS count
                        FROM Comments_Evaluations ce
                        WHERE ce.is_approved = 1
                          AND ce.product_id = :productId
                        GROUP BY ce.rating
                        """, nativeQuery = true)
        List<RatingDistributionProjection> findRatingDistributionByProductId(@Param("productId") Integer productId);

        interface RatingSummaryProjection {
                Double getAvgRating();

                Long getReviewCount();
        }

        interface RatingDistributionProjection {
                Integer getStar();

                Long getCount();
        }


}
