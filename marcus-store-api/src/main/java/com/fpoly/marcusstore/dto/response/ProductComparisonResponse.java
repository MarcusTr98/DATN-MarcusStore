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
public class ProductComparisonResponse {

    private List<ComparedProduct> products;
    private ComparisonResult result;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparedProduct {
        private Integer productId;
        private String productName;
        private String thumbnailUrl;
        private String slug;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private Integer defaultSkuId;
        private List<SpecItem> specs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecItem {
        private String specName;
        private String specValue;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonResult {
        private String overallWinner;
        private String overallReason;
        private List<UseCaseResult> useCases;
        private List<SpecWinner> specWinners;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UseCaseResult {
        private String useCase;
        private String winner;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecWinner {
        private String specName;
        private Integer winnerProductId;
        private String reason;
    }
}
