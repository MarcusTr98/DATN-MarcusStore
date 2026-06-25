package com.fpoly.marcusstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class HomeProductResponse {

    private Integer productId;
    
    private String productName;
    
    private String thumbnailUrl;
    
    private String slug;

    private Integer skuId;

    private BigDecimal price;

    private BigDecimal originalPrice;
    
    private Integer discountPercent;

    private Double rating;

    private List<String> specs;
}