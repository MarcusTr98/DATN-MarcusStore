package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryDTO {
    private Long totalSkus;
    private Long totalInStock;
    private Long totalOutOfStock;
    private Long totalLowStock;
    private Integer totalStockUnits;
    private BigDecimal totalStockValue;
    private List<LowStockResponseDTO> lowStockProducts;
}