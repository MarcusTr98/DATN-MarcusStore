package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}