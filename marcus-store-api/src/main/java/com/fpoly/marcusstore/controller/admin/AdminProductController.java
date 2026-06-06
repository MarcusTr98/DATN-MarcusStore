package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateProduct;
import com.fpoly.marcusstore.dto.request.UpdateProduct;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductResponse;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.service.ProductsService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {
    @Autowired
    ProductsService productsService;

    @GetMapping
    public Page<ProductResponse> findAllProducts(Pageable pageable) {
        return productsService.findAllProducts(pageable);
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProduct createProduct) {
        return ApiResponse.success(productsService.createProduct(createProduct));
    }

    @GetMapping("/{id}")
    public Optional<ProductResponse> getProductById(@PathVariable Integer id) {
        return productsService.getProductsById(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@Valid @PathVariable Integer id, @RequestBody UpdateProduct updateProduct) {
        return ApiResponse.success(productsService.updateProduct(id, updateProduct));
    }

    @PutMapping("/hidden/{id}")
    public Product hiddenProduct(@PathVariable Integer id) {
        return productsService.hiddenProduct(id);
    }
}
