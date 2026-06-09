package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.request.SkuSingleUpdateRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.service.ProductConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/skus")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class ProductSkuController {

    @Autowired
    private ProductConfigService configService;

    // Lấy danh sách SKU của 1 Sản phẩm
    @GetMapping("/product/{productId}")
    public ApiResponse<List<ProductSku>> getSkusByProduct(@PathVariable Integer productId) {
        return ApiResponse.success(configService.getSkusByProductId(productId));
    }

    // Nhận Ma trận SKU từ Frontend và lưu
    @PostMapping("/batch")
    public ApiResponse<String> batchCreateSkus(@Valid @RequestBody SkuBatchCreateRequest request) {
        configService.batchCreateSkus(request);
        return ApiResponse.success("Đã lưu thành công ma trận SKU!");
    }

    // Cập nhật giá, tồn kho đồng loạt
    @PutMapping("/bulk-update")
    public ApiResponse<String> bulkUpdateSkus(@RequestBody SkuBulkUpdateRequest request) {
        configService.bulkUpdateSkus(request);
        return ApiResponse.success("Cập nhật đồng loạt thành công!");
    }

    // Cập nhật 1 SKU lẻ
    @PutMapping("/{skuId}")
    public ApiResponse<ProductSku> updateSingleSku(
            @PathVariable Integer skuId,
            @Valid @RequestBody SkuSingleUpdateRequest request) {
        return ApiResponse
                .success(configService.updateSingleSku(skuId, request.getPrice(), request.getStockQuantity()));
    }

    // Xóa mềm SKU
    @DeleteMapping("/{skuId}")
    public ApiResponse<String> deleteSku(@PathVariable Integer skuId) {
        configService.deleteSku(skuId);
        return ApiResponse.success("Đã vô hiệu hóa SKU thành công!");
    }
}