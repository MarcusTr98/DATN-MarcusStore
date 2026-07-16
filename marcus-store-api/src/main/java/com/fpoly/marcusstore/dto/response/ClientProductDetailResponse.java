package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientProductDetailResponse {

    private Integer productId;
    private String productName;
    private String slug;
    private String brand;
    private String description;
    private String thumbnailUrl;
    private Boolean status;

    private String metaTitle;
    private String metaDescription;
    private String metaKeywords;

    private Integer categoryId;
    private String categoryName;
    private String categorySlug;
    private Integer parentCategoryId;
    private String parentCategoryName;
    private String parentCategorySlug;

    private List<ProductImgResponse> images;

    private List<ClientProductSkuDetailResponse> skus;

    private Integer totalSkus;
    private Integer totalStock;
    private Long totalSold;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minOriginalPrice;
    private BigDecimal maxOriginalPrice;
    private Integer minDiscountPercent;   
    private Integer maxDiscountPercent;    

    private Boolean isWished;

    private List<ClientProductSpecValueResponse> specifications;

    private Double rating;
    private Long reviewCount;
    private List<Map<String, Object>> reviewDistribution;
}