package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.HomeProductResponse;
import com.fpoly.marcusstore.service.HomeProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private HomeProductService homeProductService;

    @GetMapping
    public ApiResponse<Page<HomeProductResponse>> getHomeProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer parentCategoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String valueIds,
            @RequestParam(required = false) String brandIds,
            @RequestParam(defaultValue = "popular") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<HomeProductResponse> data = homeProductService.getHomeProducts(
                sortBy, categoryId, parentCategoryId, minPrice, maxPrice, valueIds, brandIds, pageable);
        return ApiResponse.success(data);
    }
}