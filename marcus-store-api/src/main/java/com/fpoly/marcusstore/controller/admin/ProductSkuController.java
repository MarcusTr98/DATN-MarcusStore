package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.request.SkuGenerationRequest;
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

    // 1. Lấy danh sách SKU theo Product ID
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ProductSku>>> getSkusByProduct(@PathVariable Integer productId) {
        return ResponseEntity.ok(ApiResponse.success(configService.getSkusByProductId(productId)));
    }

    // 2. Sinh SKU tự động
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateSkus(@Valid @RequestBody SkuGenerationRequest request) {
        try {
            configService.generateAndSaveProductSkus(request);
            return ResponseEntity.ok(ApiResponse.success("Đã sinh và lưu thành công các biến thể SKU!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 3. Cập nhật đồng loạt (Bulk Update)
    @PutMapping("/bulk-update")
    public ResponseEntity<ApiResponse<String>> bulkUpdateSkus(@RequestBody SkuBulkUpdateRequest request) {
        try {
            configService.bulkUpdateSkus(request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật giá và tồn kho đồng loạt thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 4. Cập nhật lẻ 1 SKU
    @PutMapping("/{skuId}")
    public ResponseEntity<ApiResponse<ProductSku>> updateSingleSku(
            @PathVariable Integer skuId,
            @Valid @RequestBody SkuSingleUpdateRequest request) {
        try {
            ProductSku updatedSku = configService.updateSingleSku(skuId, request.getPrice(),
                    request.getStockQuantity());
            return ResponseEntity.ok(ApiResponse.success(updatedSku));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 5. Xóa mềm SKU
    @DeleteMapping("/{skuId}")
    public ResponseEntity<ApiResponse<String>> deleteSku(@PathVariable Integer skuId) {
        try {
            configService.deleteSku(skuId);
            return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa SKU thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}