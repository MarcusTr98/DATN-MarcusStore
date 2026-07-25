package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustRequest {
    @NotNull(message = "Vui lòng chọn SKU")
    private Integer skuId;

    @NotNull(message = "Vui lòng nhập số lượng điều chỉnh")
    private Integer adjustmentQuantity;

    private String reason; 
}