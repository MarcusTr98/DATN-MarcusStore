package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

        // Marcus sửa
        @EntityGraph(attributePaths = { "category" })
        Page<Product> findAll(Pageable pageable);

        // Marcus sửa
        @EntityGraph(attributePaths = { "category" })
        Optional<Product> findByProductId(Integer id);

        List<Product> findByCategoryCategoryIdAndStatusTrue(Integer categoryId);

        // Tìm kiếm cơ bản cho thanh Search
        List<Product> findByProductNameContainingIgnoreCaseAndStatusTrue(String keyword);

        boolean existsByProductName(String productName);

        boolean existsBySlug(String slug);

        boolean existsByProductNameAndProductIdNot(String productName, Integer id);

        // Marcus làm lọc sp theo đk: có SKU hay ko, tìm kiếm theo tên ở màn tạo SKU
        @EntityGraph(attributePaths = { "category" })
        @Query("SELECT p FROM Product p "
                        + "WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                        + "AND (:brand IS NULL OR :brand = '' OR LOWER(p.brand) = LOWER(:brand)) " +
                        "AND (" +
                        "   (:filter = 'all') " +
                        "   OR (:filter = 'active' AND p.status = true) " +
                        "   OR (:filter = 'hidden' AND p.status = false) " +
                        "   OR (:filter = 'no_sku' AND p.skus IS EMPTY) " +
                        "   OR (:filter = 'has_sku' AND p.skus IS NOT EMPTY)" +
                        ")")
        Page<Product> findProductsWithFilter(@Param("keyword") String keyword, @Param("filter") String filter,
                        @Param("brand") String brand,
                        Pageable pageable);

        // list tên brand
        @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL AND p.brand <> ''")
        List<String> findAllDistinctBrands();
        // Lấy products theo brand, có category, status = true
        @EntityGraph(attributePaths = { "category" }) // tránh N + 1 query  load category và product cùng lúc
        @Query("SELECT p FROM Product p WHERE p.brand = :brand AND p.status = true AND p.category IS NOT NULL")
        List<Product> findByBrandAndStatusTrue(@Param("brand") String brand);

        // Gợi ý sản phẩm theo parent category cho trang Cart
        // - Lọc SP đang active (status = true)
        // - Lọc SP còn hàng: EXISTS SKU active với stockQuantity > 0
        // - Load kèm category + category.parent (1 query nhờ EntityGraph) để FE dùng nếu cần
        // - Truyền vào `categoryIds` có thể gồm cả parent_id lẫn child_id;
        //   match khi SP gắn vào category trực tiếp (category_id = id)
        //   HOẶC khi SP gắn vào child của category đó (category.parent_id = id).
        @EntityGraph(attributePaths = { "category", "category.parent" })
        @Query("SELECT DISTINCT p FROM Product p " +
                        "WHERE (p.category.categoryId IN :categoryIds " +
                        "   OR p.category.parent.categoryId IN :categoryIds) " +
                        "AND p.status = true " +
                        "AND EXISTS (SELECT 1 FROM ProductSku s " +
                        "            WHERE s.product = p " +
                        "            AND s.isActive = true " +
                        "            AND s.stockQuantity > 0)")
        List<Product> findActiveByCategoryIds(@Param("categoryIds") Collection<Integer> categoryIds);

        // Gợi ý phụ kiện cho trang Cart theo brand
        // - Lọc SP có category.parent_id = accessoryRootId (cây "Phụ kiện")
        // - Lọc brand trong tập brands (lấy từ giỏ)
        // - Lọc SP active (status = true) + có SKU active với stockQuantity > 0
        // - Load kèm category + category.parent (EntityGraph) cho FE nếu cần
        @EntityGraph(attributePaths = { "category", "category.parent" })
        @Query("SELECT DISTINCT p FROM Product p " +
                        "WHERE p.category.parent.categoryId = :accessoryRootId " +
                        "AND p.brand IN :brands " +
                        "AND p.status = true " +
                        "AND EXISTS (SELECT 1 FROM ProductSku s " +
                        "            WHERE s.product = p " +
                        "            AND s.isActive = true " +
                        "            AND s.stockQuantity > 0)")
        List<Product> findActiveAccessoriesByBrands(
                        @Param("accessoryRootId") Integer accessoryRootId,
                        @Param("brands") Collection<String> brands);
}
