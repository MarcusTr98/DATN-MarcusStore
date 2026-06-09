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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    @Autowired
    ProductsService productsService;

    // Marcus sửa lấy phân trang, tìm kiếm, lọc
    @GetMapping
    public ApiResponse<Page<ProductResponse>> findAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "all") String filter,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        // Chuyển các tham số này xuống Service xử lý
        return ApiResponse.success(productsService.findAllProducts(keyword, filter, pageable));
    }

    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProduct createProduct) {
        return ApiResponse.success(productsService.createProduct(createProduct));
    }

    // Marcus sửa: Thay vì trả về Optional, hãy trả về trực tiếp đối tượng
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Integer id) {
        return ApiResponse.success(productsService.getProductsById(id).orElse(null));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable Integer id,
            @Valid @RequestBody UpdateProduct updateProduct) {
        return ApiResponse.success(productsService.updateProduct(id, updateProduct));
    }

    @PutMapping("/hidden/{id}")
    public ApiResponse<Product> hiddenProduct(@PathVariable Integer id) {
        return ApiResponse.success(productsService.hiddenProduct(id));
    }
}
