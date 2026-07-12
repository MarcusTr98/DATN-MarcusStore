package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientSuggestedProductResponse {
    private Integer productId;
    private String productName;
    private String slug;
    private String thumbnailUrl;
    private String brand;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Boolean inStock;
    private Double rating;
    private Long reviewCount;
}
