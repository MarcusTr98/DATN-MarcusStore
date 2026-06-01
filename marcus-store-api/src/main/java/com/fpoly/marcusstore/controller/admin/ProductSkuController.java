package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.request.SkuGenerationRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.ProductConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/skus")
public class ProductSkuController {

    @Autowired
    private ProductConfigService configService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> generateSkus(@Valid @RequestBody SkuGenerationRequest request) {
        try {
            configService.generateAndSaveProductSkus(request);
            return ResponseEntity.ok(ApiResponse.success("Đã sinh và lưu thành công các biến thể SKU!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/bulk-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> bulkUpdateSkus(@RequestBody SkuBulkUpdateRequest request) {
        try {
            configService.bulkUpdateSkus(request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật giá và tồn kho đồng loạt thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}