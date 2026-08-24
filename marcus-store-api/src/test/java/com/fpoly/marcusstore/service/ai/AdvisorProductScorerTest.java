package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.dto.ai.AiAdvisorContext;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.AiProductProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvisorProductScorerTest {

    private final AdvisorProductScorer scorer = new AdvisorProductScorer();

    @Test
    void scoresVerifiedPrioritiesBudgetAndStock() {
        AiProductProjection product = product("19000000", 3);
        AiAdvisorContext context = AiAdvisorContext.builder()
                .maxBudget(new BigDecimal("20000000"))
                .priorities(List.of("CAMERA", "BATTERY"))
                .build();

        AdvisorProductScorer.ScoreResult result = scorer.score(
                product, "Camera 50MP OIS; Pin 5000mAh; Sạc 67W", context);

        assertEquals(100, result.score());
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("camera")));
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("pin/sạc")));
    }

    @Test
    void doesNotAwardPriorityPointsWithoutCatalogEvidence() {
        AiProductProjection product = product("15000000", 2);
        AiAdvisorContext context = AiAdvisorContext.builder()
                .maxBudget(new BigDecimal("20000000"))
                .priorities(List.of("CAMERA", "BATTERY"))
                .build();

        AdvisorProductScorer.ScoreResult result = scorer.score(product, "Màu sắc: Đen", context);

        assertEquals(50, result.score());
        assertTrue(result.reasons().stream().noneMatch(reason -> reason.contains("camera")));
    }

    @Test
    void penalizesProductOverBudget() {
        AiProductProjection product = product("25000000", 1);
        AiAdvisorContext context = AiAdvisorContext.builder()
                .maxBudget(new BigDecimal("20000000"))
                .priorities(List.of("PERFORMANCE"))
                .build();

        AdvisorProductScorer.ScoreResult result = scorer.score(product, "Chip Snapdragon; RAM 12GB", context);

        assertEquals(57, result.score());
        assertTrue(result.reasons().stream().noneMatch(reason -> reason.contains("Trong ngân sách")));
    }

    private AiProductProjection product(String price, int stock) {
        AiProductProjection product = mock(AiProductProjection.class);
        when(product.getPrice()).thenReturn(new BigDecimal(price));
        when(product.getStockQuantity()).thenReturn(stock);
        return product;
    }
}
