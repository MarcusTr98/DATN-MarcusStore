package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface CategoriesService {

    Page<CategoryResponse> findAllCategory(Pageable pageable);

    CategoryResponse createCategory (CreateCategory createCategory, MultipartFile file);

    Optional<CategoryResponse> getCategoryById(Integer id);

    CategoryResponse updateCategory(Integer id ,UpdateCategory updateCategory, MultipartFile file);

    CategoryResponse hiddenCategory(Integer id);

    List<CategoryResponse> getActiveChildren(Integer parentId);

    List<CategoryResponse> getMainCategoriesWithProducts();
}
