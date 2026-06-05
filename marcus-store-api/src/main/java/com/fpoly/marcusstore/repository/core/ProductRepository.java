package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategoryCategoryIdAndStatusTrue(Integer categoryId);

    // Tìm kiếm cơ bản cho thanh Search
    List<Product> findByProductNameContainingIgnoreCaseAndStatusTrue(String keyword);

    boolean existsByProductName(String productName);
    boolean existsBySlug(String slug);
    boolean existsByProductNameAndProductIdNot(String productName, Integer id);
}