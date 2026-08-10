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

    // Marcus thêm: AI lọc theo đúng cây danh mục, hãng/dòng máy, ngân sách và chỉ
    // tư vấn sản phẩm thực sự còn hàng
    @Query(value = """
            SELECT TOP 8
                p.product_id AS productId,
                p.product_name AS productName,
                p.slug AS slug,
                p.thumbnail_url AS thumbnailUrl,
                p.brand AS brand,
                c.category_name AS categoryName,
                parent.category_name AS parentCategoryName,
                sku.price AS price,
                sku.max_price AS maxPrice,
                sku.stock_quantity AS stockQuantity
            FROM Products p
            INNER JOIN (
                SELECT s.product_id,
                       MIN(s.price) AS price,
                       MAX(s.price) AS max_price,
                       SUM(COALESCE(s.stock_quantity, 0)) AS stock_quantity
                FROM Product_Skus s
                -- Marcus sửa: giá AI phải là giá của SKU thực sự còn hàng, không
                -- lấy MIN(price) từ một SKU active nhưng đã hết tồn.
                WHERE s.is_active = 1
                  AND COALESCE(s.stock_quantity, 0) > 0
                GROUP BY s.product_id
            ) sku ON sku.product_id = p.product_id
            INNER JOIN Categories c ON c.category_id = p.category_id
            LEFT JOIN Categories parent ON parent.category_id = c.parent_id
            WHERE p.status = 1
              AND sku.stock_quantity > 0
              AND (:categoryKeyword = ''
                   OR LOWER(c.category_name) LIKE LOWER(CONCAT('%', :categoryKeyword, '%'))
                   OR LOWER(COALESCE(parent.category_name, '')) LIKE LOWER(CONCAT('%', :categoryKeyword, '%')))
              AND (:keyword = '' OR LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:minPrice IS NULL OR sku.price >= :minPrice)
              AND (:maxPrice IS NULL OR sku.price <= :maxPrice)
            ORDER BY
                CASE WHEN LOWER(p.product_name) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 0 ELSE 1 END,
                CASE WHEN :targetPrice IS NULL THEN 0 ELSE ABS(sku.price - :targetPrice) END,
                -- Marcus sửa: câu hỏi giá/hãng không có mức mục tiêu sẽ hiển
                -- thị lựa chọn còn hàng có giá thấp trước.
                sku.price ASC,
                p.created_at DESC
            """, nativeQuery = true)
    List<AiProductProjection> findProductsForAiAdvisor(
            @Param("keyword") String keyword,
            @Param("categoryKeyword") String categoryKeyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("targetPrice") BigDecimal targetPrice);

    // Marcus thêm: câu “máy này/con này” đọc đúng sản phẩm khách đã click, không
    // chạy lại xếp hạng rồi tự chuyển sang model khác.
    @Query(value = """
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                p.slug AS slug,
                p.thumbnail_url AS thumbnailUrl,
                p.brand AS brand,
                c.category_name AS categoryName,
                parent.category_name AS parentCategoryName,
                MIN(s.price) AS price,
                MAX(s.price) AS maxPrice,
                SUM(COALESCE(s.stock_quantity, 0)) AS stockQuantity
            FROM Products p
            INNER JOIN Product_Skus s ON s.product_id = p.product_id
            INNER JOIN Categories c ON c.category_id = p.category_id
            LEFT JOIN Categories parent ON parent.category_id = c.parent_id
            WHERE p.product_id = :productId
              AND p.status = 1
              AND s.is_active = 1
              AND COALESCE(s.stock_quantity, 0) > 0
            GROUP BY p.product_id, p.product_name, p.slug, p.thumbnail_url, p.brand,
                     c.category_name, parent.category_name
            """, nativeQuery = true)
    java.util.Optional<AiProductProjection> findFocusedProductForAiAdvisor(
            @Param("productId") Integer productId);

    // Marcus thêm: trả đúng lựa chọn SKU còn hàng (màu/dung lượng/giá), không chỉ
    // báo khoảng giá chung của Product.
    @Query(value = """
            SELECT
                s.product_id AS productId,
                s.sku_id AS skuId,
                s.sku_code AS skuCode,
                s.price AS price,
                s.stock_quantity AS stockQuantity,
                STRING_AGG(
                    CASE WHEN a.attribute_name IS NULL OR av.value_string IS NULL THEN NULL
                         ELSE CONCAT(a.attribute_name, ': ', av.value_string) END,
                    ', '
                ) AS attributes
            FROM Product_Skus s
            LEFT JOIN Sku_Attribute_Values sav ON sav.sku_id = s.sku_id
            LEFT JOIN Attribute_Values av ON av.value_id = sav.value_id
            LEFT JOIN Attributes a ON a.attribute_id = av.attribute_id
            WHERE s.product_id IN :productIds
              AND s.is_active = 1
              AND COALESCE(s.stock_quantity, 0) > 0
            GROUP BY s.product_id, s.sku_id, s.sku_code, s.price, s.stock_quantity
            ORDER BY s.product_id, s.price, s.sku_id
            """, nativeQuery = true)
    List<AiSkuProjection> findAvailableSkusForAiAdvisor(
            @Param("productIds") List<Integer> productIds);

    // Marcus thêm: cung cấp thông số thật để AI so sánh có căn cứ, không tự bịa
    // cấu hình sản phẩm
    @Query(value = """
            SELECT
                psv.product_id AS productId,
                sa.name AS specName,
                psv.value_text AS specValue,
                sa.unit AS unit
            FROM Product_Spec_Values psv
            INNER JOIN Spec_Attributes sa
                ON sa.spec_attribute_id = psv.spec_attribute_id
            WHERE psv.product_id IN :productIds
            ORDER BY psv.product_id, sa.display_order, sa.spec_attribute_id
            """, nativeQuery = true)
    List<AiProductSpecProjection> findProductSpecsForAiAdvisor(
            @Param("productIds") List<Integer> productIds);

    /**
     * Lấy SKU rẻ nhất còn active cho danh sách productIds (dùng cho Wishlist, tránh
     * query phức tạp + ORDER BY duplicate column của findHomeProductRawData)
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

        String getBrand();

        String getCategoryName();

        String getParentCategoryName();

        BigDecimal getPrice();

        BigDecimal getMaxPrice();

        Integer getStockQuantity();
    }

    interface AiProductSpecProjection {
        Integer getProductId();

        String getSpecName();

        String getSpecValue();

        String getUnit();
    }

    interface AiSkuProjection {
        Integer getProductId();

        Integer getSkuId();

        String getSkuCode();

        BigDecimal getPrice();

        Integer getStockQuantity();

        String getAttributes();
    }
}
