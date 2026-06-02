package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Lấy các danh mục gốc (không có cha) để làm Menu
    List<Category> findByParentIsNullAndStatusTrue();

    boolean existsCategoriesByCategoryName(String categoryName);
}