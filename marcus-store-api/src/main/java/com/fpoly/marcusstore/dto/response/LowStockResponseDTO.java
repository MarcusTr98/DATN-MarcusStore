package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LowStockResponseDTO {
    private String skuCode;
    private String productName;
    private String brand;
    private Integer stockQuantity;
    private String status;
}