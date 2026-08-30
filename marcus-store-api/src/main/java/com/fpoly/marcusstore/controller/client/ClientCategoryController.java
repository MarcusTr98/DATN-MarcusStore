package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.dto.response.ClientFilterGroupResponse;
import com.fpoly.marcusstore.service.CategoriesService;
import com.fpoly.marcusstore.service.ProductFilterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/client/categories")
public class ClientCategoryController {

    @Autowired
    CategoriesService categoriesService;

    @Autowired
    ProductFilterService productFilterService;

    @GetMapping("/{parentId}/children")
    public ApiResponse<List<CategoryResponse>> getActiveChildren(@PathVariable Integer parentId) {
        return ApiResponse.success(categoriesService.getActiveChildren(parentId));
    }

    @GetMapping("/by-slug/{parentSlug}/children")
    public ApiResponse<List<CategoryResponse>> getActiveChildrenBySlug(@PathVariable String parentSlug) {
        return ApiResponse.success(categoriesService.getActiveChildrenBySlug(parentSlug));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return categoriesService.getCategoryBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Danh mục không tồn tại"));
    }

    @GetMapping("/main")
    public ApiResponse<List<CategoryResponse>> getMainCategoriesWithProducts() {
        return ApiResponse.success(categoriesService.getMainCategoriesWithProducts());
    }

    @GetMapping("/{parentId}/filters")
    public ApiResponse<List<ClientFilterGroupResponse>> getFilters(@PathVariable Integer parentId) {
        return ApiResponse.success(productFilterService.getFiltersForCategory(parentId));
    }
}
