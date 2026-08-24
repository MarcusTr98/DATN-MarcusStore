package com.fpoly.marcusstore.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AiAdvisorResponse {
    // Marcus thêm: mã ẩn danh cho từng câu trả lời để mỗi lượt chỉ feedback một
    // lần.
    private String adviceId;
    private String answer;
    private List<ProductSuggestion> products;
    private boolean fallbackUsed;
    private String source;
    private AiAdvisorContext context;
    private AdviceSections sections;
    private ProductComparison comparison;

    public record ProductComparison(
            List<Integer> productIds,
            List<String> productNames,
            List<ComparisonRow> rows,
            Integer bestProductId) {
    }

    public record ComparisonRow(String label, List<String> values) {
    }

    @Data
    @Builder
    public static class AdviceSections {
        private String needSummary;
        private List<String> suggestions;
        private List<String> considerations;
        private Integer bestProductId;
        private String bestReason;
        private String followUpQuestion;
    }

    @Data
    @Builder
    public static class ProductSuggestion {
        private Integer productId;
        private String productName;
        private String slug;
        private String thumbnailUrl;
        private BigDecimal price;
        private BigDecimal maxPrice;
        private boolean inStock;
        private Integer compatibilityScore;
        private List<String> matchReasons;
        private List<SkuSuggestion> skuOptions;
        private Integer matchedSkuId;
    }

    @Data
    @Builder
    public static class SkuSuggestion {
        private Integer skuId;
        private String skuCode;
        private BigDecimal price;
        private Integer stockQuantity;
        private String attributes;
    }
}
