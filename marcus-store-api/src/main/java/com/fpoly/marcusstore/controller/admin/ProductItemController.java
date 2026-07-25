package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.ProductItemUpdateRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProductItemResponse;
import com.fpoly.marcusstore.service.ProductItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
public class ProductItemController {

    @Autowired
    private ProductItemService productItemService;

    @GetMapping("/{skuId}/items")
    public ApiResponse<List<ProductItemResponse>> getItemsBySku(@PathVariable Integer skuId) {
        return ApiResponse.success(productItemService.getBySku(skuId));
    }

    @PostMapping("/{skuId}/items")
    public ApiResponse<ProductItemResponse> createItem(
            @PathVariable Integer skuId,
            @Valid @RequestBody ProductItemUpdateRequest request) {
        return ApiResponse.success(productItemService.create(skuId, request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<ProductItemResponse> updateItem(
            @PathVariable Integer itemId,
            @Valid @RequestBody ProductItemUpdateRequest request) {
        return ApiResponse.success(productItemService.update(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Integer itemId) {
        productItemService.delete(itemId);
        return ApiResponse.success(null);
    }
}
