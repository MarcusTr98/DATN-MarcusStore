package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientProductSkuDetailResponse {

    private Integer skuId;
    private String skuCode;
    private String skuImageUrl;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Integer weightGram;
    private Integer stockQuantity;
    private Boolean isActive;
    private Boolean inStock;
    private Boolean lowStock;

    private List<ClientSkuAttributeValueResponse> attributeValues;
}
