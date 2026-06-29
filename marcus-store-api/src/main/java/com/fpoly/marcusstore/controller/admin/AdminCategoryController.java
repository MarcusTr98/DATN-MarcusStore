package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.service.CategoriesService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    @Autowired
    CategoriesService categoriesService;

    @GetMapping
    public ApiResponse<Page<CategoryResponse>> findAllCategory(Pageable pageable) {
        return ApiResponse.success(categoriesService.findAllCategory(pageable));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<CategoryResponse> createCategory(
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "parentId", required = false) Integer parentId,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        CreateCategory createCategory = new CreateCategory();
        createCategory.setCategoryName(categoryName);
        createCategory.setParentId(parentId);
        return ApiResponse.success(categoriesService.createCategory(createCategory, file));
    }

    @GetMapping("/{id}")
    public ApiResponse<Optional<CategoryResponse>> getCategoryById(@PathVariable Integer id) {
        return ApiResponse.success(categoriesService.getCategoryById(id));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<CategoryResponse> updateCategory(
            @PathVariable Integer id,
            @RequestParam("categoryName") String categoryName,
            @RequestParam(value = "parentId", required = false) Integer parentId,
            @RequestParam("status") Boolean status,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        UpdateCategory updateCategory = new UpdateCategory();
        updateCategory.setCategoryName(categoryName);
        updateCategory.setParentId(parentId);
        updateCategory.setStatus(status);
        return ApiResponse.success(categoriesService.updateCategory(id, updateCategory, file));
    }

    @PutMapping("/hidden/{id}")
    public ApiResponse<CategoryResponse> hiddenCategory(@PathVariable Integer id) {
        return ApiResponse.success(categoriesService.hiddenCategory(id));
    }
}
