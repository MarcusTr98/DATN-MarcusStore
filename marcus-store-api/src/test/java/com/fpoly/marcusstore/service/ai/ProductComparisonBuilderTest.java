package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiAdvisorResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductComparisonBuilderTest {

    private final ProductComparisonBuilder builder = new ProductComparisonBuilder();

    @Test
    void buildsAlignedMatrixAndMarksMissingCatalogValue() {
        var first = product(1, "Máy A", "10000000", 88);
        var second = product(2, "Máy B", "12000000", 75);

        var result = builder.build(List.of(first, second), Map.of(
                1, "Chip: Snapdragon 8, Pin: 5000 mAh",
                2, "Chip: Dimensity 9200"));

        assertEquals(List.of("Máy A", "Máy B"), result.productNames());
        assertEquals(1, result.bestProductId());
        var battery = result.rows().stream().filter(row -> row.label().equals("Pin")).findFirst().orElseThrow();
        assertEquals(List.of("5000 mAh", "Chưa có dữ liệu"), battery.values());
    }

    @Test
    void limitsComparisonToThreeProducts() {
        var result = builder.build(List.of(
                product(1, "A", "1", 60), product(2, "B", "2", 70),
                product(3, "C", "3", 80), product(4, "D", "4", 90)), Map.of());

        assertEquals(3, result.productIds().size());
        assertTrue(result.productIds().stream().noneMatch(id -> id == 4));
    }

    private AiAdvisorResponse.ProductSuggestion product(int id, String name, String price, int score) {
        return AiAdvisorResponse.ProductSuggestion.builder()
                .productId(id).productName(name).price(new BigDecimal(price))
                .inStock(true).compatibilityScore(score).build();
    }
}
