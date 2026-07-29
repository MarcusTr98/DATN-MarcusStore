package com.fpoly.marcusstore.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AiAdvisorResponse {
    private String answer;
    private List<ProductSuggestion> products;

    @Data
    @Builder
    public static class ProductSuggestion {
        private Integer productId;
        private String productName;
        private String slug;
        private String thumbnailUrl;
        private BigDecimal price;
        private boolean inStock;
    }
}
