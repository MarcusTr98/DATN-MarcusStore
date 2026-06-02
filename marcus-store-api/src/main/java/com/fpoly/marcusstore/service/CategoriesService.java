package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface CategoriesService {

    Page<Map<String, Object>> findAllCategory(Pageable pageable);

    Category createCategory(Category category);

    Optional<Map<String, Object>> getCategoryById(Integer id);

    Category updateCategory(Integer id ,Category category);

    Category hiddenCategory(Integer id);
}
