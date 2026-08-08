package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.InventoryAdjustRequest;
import com.fpoly.marcusstore.dto.request.StockImportRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.InventoryResponse;
import com.fpoly.marcusstore.dto.response.InventorySummaryDTO;
import com.fpoly.marcusstore.dto.response.InventoryTransactionResponse;
import com.fpoly.marcusstore.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
public class AdminInventoryController {

    @Autowired
    private InventoryService inventorySer;

    @GetMapping("/inventory")
    public ApiResponse<Page<InventoryResponse>> getInventoryList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stockStatus,
            @RequestParam(required = false) Boolean hasImei,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("skuId").descending());
        Page<InventoryResponse> data = inventorySer.getInventoryList(keyword, stockStatus, hasImei, pageable);
        return ApiResponse.success(data);
    }

    @GetMapping("/summary")
    public ApiResponse<InventorySummaryDTO> getInventorySummary(
            @RequestParam(required = false) Boolean hasImei) {
        return ApiResponse.success(inventorySer.getInventorySummary(hasImei));
    }

    @GetMapping("/{skuId}")
    public ApiResponse<InventoryResponse> getSkuDetail(@PathVariable Integer skuId) {
        return ApiResponse.success(inventorySer.getSkuDetail(skuId));
    }

    @PostMapping("/import")
    public ApiResponse<InventoryTransactionResponse> importStock(
            @Valid @RequestBody StockImportRequest request) {
        return ApiResponse.success(inventorySer.importStock(request));
    }

    @PutMapping("/adjust")
    public ApiResponse<InventoryTransactionResponse> adjustStock(
            @Valid @RequestBody InventoryAdjustRequest request) {
        return ApiResponse.success(inventorySer.adjustStock(request));
    }
}
