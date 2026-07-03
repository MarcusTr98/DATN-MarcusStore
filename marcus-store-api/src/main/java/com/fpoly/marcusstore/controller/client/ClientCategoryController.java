package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.dto.response.ClientFilterGroupResponse;
import com.fpoly.marcusstore.service.CategoriesService;
import com.fpoly.marcusstore.service.ProductFilterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/main")
    public ApiResponse<List<CategoryResponse>> getMainCategoriesWithProducts() {
        return ApiResponse.success(categoriesService.getMainCategoriesWithProducts());
    }

    @GetMapping("/{parentId}/filters")
    public ApiResponse<List<ClientFilterGroupResponse>> getFilters(@PathVariable Integer parentId) {
        return ApiResponse.success(productFilterService.getFiltersForCategory(parentId));
    }
}
