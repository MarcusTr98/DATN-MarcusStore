package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlashSaleItemRequest {
    @NotNull(message = "SKU không được để trống")
    private Integer SkuId;
    @NotNull(message = "Giá gốc không được để trống")
    private BigDecimal originalPrice;
    @NotNull(message = "Giá flash sale không được để trống")
    private BigDecimal flashSalePrice;
    @NotNull(message = "Số lượng flash sale không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer flashSaleQuantity;
}
