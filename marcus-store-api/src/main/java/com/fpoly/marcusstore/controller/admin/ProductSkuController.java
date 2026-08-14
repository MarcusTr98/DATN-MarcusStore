package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.request.SkuSingleUpdateRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.SkuImageUpdateResponse;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.service.ProductConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/skus")
@PreAuthorize("hasAuthority('SKU_CREATE')")
@Validated
public class ProductSkuController {

    @Autowired
    private ProductConfigService configService;

    // Lấy danh sách SKU của 1 Sản phẩm
    @GetMapping("/product/{productId}")
    public ApiResponse<List<ProductSku>> getSkusByProduct(@PathVariable @Positive Integer productId) {
        return ApiResponse.success(configService.getSkusByProductId(productId));
    }

    // Nhận Ma trận SKU từ Frontend và lưu
    @PostMapping("/batch")
    public ApiResponse<String> batchCreateSkus(@Valid @RequestBody SkuBatchCreateRequest request) {
        configService.batchCreateSkus(request);
        return ApiResponse.success("Đã lưu thành công ma trận SKU!");
    }

    // Marcus sửa: cập nhật giá niêm yết và giá bán đồng loạt; tồn kho do module
    // kho/IMEI quản lý riêng.
    @PutMapping("/bulk-update")
    public ApiResponse<String> bulkUpdateSkus(@Valid @RequestBody SkuBulkUpdateRequest request) {
        configService.bulkUpdateSkus(request);
        return ApiResponse.success("Cập nhật đồng loạt thành công!");
    }

    // Cập nhật 1 SKU lẻ
    @PutMapping("/{skuId}")
    public ApiResponse<ProductSku> updateSingleSku(
            @PathVariable @Positive Integer skuId,
            @Valid @RequestBody SkuSingleUpdateRequest request) {
        return ApiResponse
                .success(configService.updateSingleSku(skuId, request.getOriginalPrice(), request.getPrice()));
    }

    // Marcus thêm: ảnh đại diện biến thể được lưu tại Product_Skus.sku_image_url;
    // không tạo bản ghi trùng trong Product_Images.
    @PostMapping(value = "/images", consumes = "multipart/form-data")
    public ApiResponse<List<SkuImageUpdateResponse>> uploadSkuImage(
            @RequestParam("skuIds") List<@Positive Integer> skuIds,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.success(configService.updateSkuImages(skuIds, file));
    }

    // Xóa mềm SKU
    @DeleteMapping("/{skuId}")
    public ApiResponse<String> deleteSku(@PathVariable @Positive Integer skuId) {
        configService.deleteSku(skuId);
        return ApiResponse.success("Đã vô hiệu hóa SKU thành công!");
    }
}
