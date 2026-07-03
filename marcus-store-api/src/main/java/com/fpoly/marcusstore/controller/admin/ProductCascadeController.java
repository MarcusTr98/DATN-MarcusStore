package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductCascadeResponse;
import com.fpoly.marcusstore.service.ProductCascadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")

public class ProductCascadeController {

    @Autowired
    private ProductCascadeService productCascadeService;


    // Mặc định ẩn SKU hết hàng (stockQuantity <= 0).

    @GetMapping("/cascade")
    public ApiResponse<List<ProductCascadeResponse>> getAll(
            @RequestParam(name = "includeOutOfStock", defaultValue = "false")
            boolean includeOutOfStock) {
        return ApiResponse.success(
                productCascadeService.getAllProductCascade(includeOutOfStock));
    }


    // Trả 404 nếu brand không có sản phẩm active.

    @GetMapping("/cascade/{brand}")
    public ApiResponse<ProductCascadeResponse> getByBrand(
            @PathVariable String brand,
            @RequestParam(name = "includeOutOfStock", defaultValue = "false")
            boolean includeOutOfStock) {
        ProductCascadeResponse data = productCascadeService
                .getProductCascadeByBrand(brand, includeOutOfStock);
        if (data == null) {
            return ApiResponse.error(404,
                    "Brand '" + brand + "' không tồn tại hoặc không có sản phẩm đang bán");
        }
        return ApiResponse.success(data);
    }
}
