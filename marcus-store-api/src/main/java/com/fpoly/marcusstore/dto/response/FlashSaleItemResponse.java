package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleItemResponse {
    private Integer skuId;
    private Integer productId;
    private String productName;
    private String skuCode;
    private String skuImageUrl;
    private BigDecimal originalPrice;
    private BigDecimal flashSalePrice;
    private Integer flashSaleQuantity;
    private Integer soldQuantity;
    private Integer remainingQuantity;
    private LocalDateTime createdAt;
}
