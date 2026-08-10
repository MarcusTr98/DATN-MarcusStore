package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Integer skuId;
    private String skuCode;
    private String skuImageUrl;
    private String productName;
    private String categoryName;
    private String brand;
    private BigDecimal price;
    private Integer stockQuantity;
    private Boolean isActive;
    private String stockStatus;
    private Boolean statusImei;
    private String warehouseType;
}
