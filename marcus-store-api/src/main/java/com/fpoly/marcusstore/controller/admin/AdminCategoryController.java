package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.service.CategoriesService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    @Autowired
    CategoriesService categoriesService;

    @GetMapping
    public Page<CategoryResponse> findAllCategory(Pageable pageable) {
        return categoriesService.findAllCategory(pageable);
    }

    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CreateCategory createCategory) {
        return ApiResponse.success(categoriesService.createCategory(createCategory));
    }

    @GetMapping("/{id}")
    public Optional<CategoryResponse> getCategoryById(@PathVariable Integer id) {
        return categoriesService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@Valid @PathVariable Integer id, @RequestBody UpdateCategory updateCategory) {
        return ApiResponse.success(categoriesService.updateCategory(id, updateCategory));
    }

    @PutMapping("/hidden/{id}")
    public Category hiddenCategory(@PathVariable Integer id) {
        return categoriesService.hiddenCategory(id);
    }
}
