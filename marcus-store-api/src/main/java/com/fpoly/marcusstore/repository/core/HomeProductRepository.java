package com.fpoly.marcusstore.repository.core;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.core.Product;

@Repository
public interface HomeProductRepository extends JpaRepository<Product, Integer> {

    @Query(value = """
            SELECT
                p.product_id      AS productId,
                p.product_name    AS productName,
                p.thumbnail_url   AS thumbnailUrl,
                p.slug            AS slug,
                sku.sku_id        AS skuId,
                sku.price         AS price,
                sku.original_price AS originalPrice
            FROM Products p
            INNER JOIN (
                SELECT s.*,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id ORDER BY s.price ASC) AS rn
                FROM Product_Skus s
                WHERE s.is_active = 1
            ) sku ON sku.product_id = p.product_id AND sku.rn = 1
            WHERE p.status = 1
              AND (
                  :brandCategoryId IS NOT NULL AND (
                      p.category_id = :brandCategoryId
                      OR p.category_id IN (
                          SELECT child.category_id FROM Categories child WHERE child.parent_id = :brandCategoryId
                      )
                  )
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NULL AND :brandIdsCsv IS NOT NULL AND :brandIdsCsv != ''
                     AND (
                         p.category_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         OR p.category_id IN (
                             SELECT child.category_id FROM Categories child
                             WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         )
                     )
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NULL AND (:brandIdsCsv IS NULL OR :brandIdsCsv = '')
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NOT NULL AND (
                      (:brandIdsCsv IS NULL OR :brandIdsCsv = '') AND (
                          p.category_id = :parentCategoryId
                          OR p.category_id IN (
                              SELECT child.category_id FROM Categories child WHERE child.parent_id = :parentCategoryId
                          )
                      )
                      OR (:brandIdsCsv IS NOT NULL AND :brandIdsCsv != '') AND (
                          p.category_id IN (
                              SELECT child.category_id FROM Categories child
                              WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                          )
                          OR p.category_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                      )
                  )
                  OR :brandCategoryId IS NOT NULL AND :brandIdsCsv IS NOT NULL AND :brandIdsCsv != ''
                     AND p.category_id IN (
                         SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ',')
                         UNION
                         SELECT child.category_id FROM Categories child
                         WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         UNION
                         SELECT :brandCategoryId
                     )
              )
              AND (:minPrice IS NULL OR sku.price >= :minPrice)
              AND (:maxPrice IS NULL OR sku.price <= :maxPrice OR :maxPrice = -1)
              AND (
                  :valueIdsCsv IS NULL OR :valueIdsCsv = ''
                  OR NOT EXISTS (
                      SELECT 1
                      FROM (
                          SELECT av_inner.attribute_id AS aid
                          FROM Attribute_Values av_inner
                          WHERE av_inner.value_id IN (
                              SELECT CAST(value AS INT) FROM STRING_SPLIT(:valueIdsCsv, ',')
                          )
                          GROUP BY av_inner.attribute_id
                      ) req_attr
                      WHERE NOT EXISTS (
                          SELECT 1
                          FROM Product_Skus ps
                          JOIN Sku_Attribute_Values sav2 ON sav2.sku_id = ps.sku_id
                          WHERE ps.product_id = p.product_id
                            AND ps.is_active = 1
                            AND sav2.value_id IN (
                                SELECT CAST(value AS INT) FROM STRING_SPLIT(:valueIdsCsv, ',')
                            )
                            AND sav2.value_id IN (
                                SELECT av2.value_id FROM Attribute_Values av2 WHERE av2.attribute_id = req_attr.aid
                            )
                      )
                  )
              )
            ORDER BY
                CASE WHEN :sortBy = 'price_asc'  THEN sku.price END ASC,
                CASE WHEN :sortBy = 'price_desc' THEN sku.price END DESC,
                CASE WHEN :sortBy = 'discount'   THEN
                    CASE WHEN sku.original_price > sku.price
                         THEN (sku.original_price - sku.price) * 100.0 / sku.original_price
                         ELSE 0 END
                END DESC,
                CASE WHEN :sortBy = 'popular' OR :sortBy IS NULL THEN p.created_at END DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM Products p
            INNER JOIN (
                SELECT s.*,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id ORDER BY s.price ASC) AS rn
                FROM Product_Skus s
                WHERE s.is_active = 1
            ) sku ON sku.product_id = p.product_id AND sku.rn = 1
            WHERE p.status = 1
              AND (
                  :brandCategoryId IS NOT NULL AND (
                      p.category_id = :brandCategoryId
                      OR p.category_id IN (
                          SELECT child.category_id FROM Categories child WHERE child.parent_id = :brandCategoryId
                      )
                  )
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NULL AND :brandIdsCsv IS NOT NULL AND :brandIdsCsv != ''
                     AND (
                         p.category_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         OR p.category_id IN (
                             SELECT child.category_id FROM Categories child
                             WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         )
                     )
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NULL AND (:brandIdsCsv IS NULL OR :brandIdsCsv = '')
                  OR :brandCategoryId IS NULL AND :parentCategoryId IS NOT NULL AND (
                      (:brandIdsCsv IS NULL OR :brandIdsCsv = '') AND (
                          p.category_id = :parentCategoryId
                          OR p.category_id IN (
                              SELECT child.category_id FROM Categories child WHERE child.parent_id = :parentCategoryId
                          )
                      )
                      OR (:brandIdsCsv IS NOT NULL AND :brandIdsCsv != '') AND (
                          p.category_id IN (
                              SELECT child.category_id FROM Categories child
                              WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                          )
                          OR p.category_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                      )
                  )
                  OR :brandCategoryId IS NOT NULL AND :brandIdsCsv IS NOT NULL AND :brandIdsCsv != ''
                     AND p.category_id IN (
                         SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ',')
                         UNION
                         SELECT child.category_id FROM Categories child
                         WHERE child.parent_id IN (SELECT CAST(value AS INT) FROM STRING_SPLIT(:brandIdsCsv, ','))
                         UNION
                         SELECT :brandCategoryId
                     )
              )
              AND (:minPrice IS NULL OR sku.price >= :minPrice)
              AND (:maxPrice IS NULL OR sku.price <= :maxPrice OR :maxPrice = -1)
              AND (
                  :valueIdsCsv IS NULL OR :valueIdsCsv = ''
                  OR NOT EXISTS (
                      SELECT 1
                      FROM (
                          SELECT av_inner.attribute_id AS aid
                          FROM Attribute_Values av_inner
                          WHERE av_inner.value_id IN (
                              SELECT CAST(value AS INT) FROM STRING_SPLIT(:valueIdsCsv, ',')
                          )
                          GROUP BY av_inner.attribute_id
                      ) req_attr
                      WHERE NOT EXISTS (
                          SELECT 1
                          FROM Product_Skus ps
                          JOIN Sku_Attribute_Values sav2 ON sav2.sku_id = ps.sku_id
                          WHERE ps.product_id = p.product_id
                            AND ps.is_active = 1
                            AND sav2.value_id IN (
                                SELECT CAST(value AS INT) FROM STRING_SPLIT(:valueIdsCsv, ',')
                            )
                            AND sav2.value_id IN (
                                SELECT av2.value_id FROM Attribute_Values av2 WHERE av2.attribute_id = req_attr.aid
                            )
                      )
                  )
              )
            """, nativeQuery = true)
    Page<HomeProductRawProjection> findHomeProductRawData(
            @Param("sortBy") String sortBy,
            @Param("brandCategoryId") Integer brandCategoryId,
            @Param("parentCategoryId") Integer parentCategoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("valueIdsCsv") String valueIdsCsv,
            @Param("brandIdsCsv") String brandIdsCsv,
            Pageable pageable);

    @Query(value = """
            SELECT
                ce.product_id AS productId,
                AVG(CAST(ce.rating AS FLOAT)) AS avgRating,
                COUNT(ce.review_id) AS reviewCount
            FROM Comments_Evaluations ce
            WHERE ce.is_approved = 1
              AND ce.product_id IN :productIds
            GROUP BY ce.product_id
            """, nativeQuery = true)
    List<RatingProjection> findRatingDataByProductIds(@Param("productIds") List<Integer> productIds);

    @Query(value = """
            SELECT
                s.product_id AS productId,
                av.value_string AS valueString
            FROM Product_Skus s
            JOIN Sku_Attribute_Values sav ON sav.sku_id = s.sku_id
            JOIN Attribute_Values av ON av.value_id = sav.value_id
            WHERE s.sku_id IN (
                SELECT TOP 1 s2.sku_id
                FROM Product_Skus s2
                WHERE s2.product_id = s.product_id AND s2.is_active = 1
                ORDER BY s2.price ASC
            )
            AND s.product_id IN :productIds
            """, nativeQuery = true)
    List<SpecProjection> findSpecsByProductIds(@Param("productIds") List<Integer> productIds);

    @Query(value = """
            SELECT TOP 10
                p.product_id       AS productId,
                p.product_name     AS productName,
                p.thumbnail_url    AS thumbnailUrl,
                p.slug             AS slug,
                sku.sku_id         AS skuId,
                sku.price          AS price,
                sku.original_price AS originalPrice
            FROM Products p
            INNER JOIN (
                SELECT s.product_id, s.sku_id, s.price, s.original_price,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id ORDER BY s.price ASC) AS rn
                FROM Product_Skus s
                WHERE s.is_active = 1
            ) sku ON sku.product_id = p.product_id AND sku.rn = 1
            WHERE p.status = 1
            ORDER BY p.created_at DESC
            """, nativeQuery = true)
    List<HomeProductRawProjection> findNewestProducts();

    // Marcus thêm: lấy ngữ cảnh sản phẩm gọn cho AI, chỉ gồm SKU active và giá thấp
    // nhất.
    @Query(value = """
            SELECT TOP 6
                p.product_id AS productId,
                p.product_name AS productName,
                p.slug AS slug,
                p.thumbnail_url AS thumbnailUrl,
                sku.price AS price,
                sku.stock_quantity AS stockQuantity
            FROM Products p
            INNER JOIN (
                SELECT s.*,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id ORDER BY s.price ASC) AS rn
                FROM Product_Skus s
                WHERE s.is_active = 1
            ) sku ON sku.product_id = p.product_id AND sku.rn = 1
            WHERE p.status = 1
              AND (:keyword = '' OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY
                CASE WHEN LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 0 ELSE 1 END,
                p.created_at DESC
            """, nativeQuery = true)
    List<AiProductProjection> findProductsForAiAdvisor(@Param("keyword") String keyword);

    /**
     * Lấy SKU rẻ nhất còn active cho danh sách productIds (dùng cho Wishlist, tránh
     * query phức tạp + ORDER BY duplicate column của findHomeProductRawData).
     */
    @Query(value = """
            SELECT
                p.product_id       AS productId,
                p.product_name     AS productName,
                p.thumbnail_url    AS thumbnailUrl,
                p.slug             AS slug,
                sku.sku_id         AS skuId,
                sku.price          AS price,
                sku.original_price AS originalPrice
            FROM Products p
            INNER JOIN (
                SELECT s.product_id, s.sku_id, s.price, s.original_price,
                       ROW_NUMBER() OVER (PARTITION BY s.product_id ORDER BY s.price ASC) AS rn
                FROM Product_Skus s
                WHERE s.is_active = 1
            ) sku ON sku.product_id = p.product_id AND sku.rn = 1
            WHERE p.product_id IN :productIds
            """, nativeQuery = true)
    List<HomeProductRawProjection> findSkuOverviewByProductIds(@Param("productIds") List<Integer> productIds);

    interface HomeProductRawProjection {
        Integer getProductId();

        String getProductName();

        String getThumbnailUrl();

        String getSlug();

        Integer getSkuId();

        java.math.BigDecimal getPrice();

        java.math.BigDecimal getOriginalPrice();
    }

    interface RatingProjection {
        Integer getProductId();

        Double getAvgRating();

        Long getReviewCount();
    }

    interface SpecProjection {
        Integer getProductId();

        String getValueString();
    }

    interface AiProductProjection {
        Integer getProductId();

        String getProductName();

        String getSlug();

        String getThumbnailUrl();

        BigDecimal getPrice();

        Integer getStockQuantity();
    }
}
