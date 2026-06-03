package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.entity.core.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CategoriesService {

    Page<CategoryResponse> findAllCategory(Pageable pageable);

    CategoryResponse createCategory (CreateCategory createCategory);

    Optional<CategoryResponse> getCategoryById(Integer id);

    CategoryResponse updateCategory(Integer id ,UpdateCategory updateCategory);

    Category hiddenCategory(Integer id);
}
