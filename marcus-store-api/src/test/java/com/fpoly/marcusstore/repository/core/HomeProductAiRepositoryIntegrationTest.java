package com.fpoly.marcusstore.repository.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class HomeProductAiRepositoryIntegrationTest {

    @Autowired
    private HomeProductRepository repository;

    @Test
    void aiCatalogQueriesReadFocusedProductAndAvailableSkuAttributes() {
        // Marcus thêm: kiểm tra trực tiếp SQL Server để query AI mới không chỉ
        // compile mà còn chạy được với schema Product/SKU/Attribute hiện tại.
        var products = assertDoesNotThrow(() -> repository.findProductsForAiAdvisor(
                "", "", null, null, null));
        if (products.isEmpty())
            return;

        Integer productId = products.getFirst().getProductId();
        assertTrue(repository.findFocusedProductForAiAdvisor(productId).isPresent());
        assertDoesNotThrow(() -> repository.findAvailableSkusForAiAdvisor(List.of(productId)));
    }

    @Test
    void aiCatalogLexiconReadsAvailablePhoneBrandsAndModels() {
        var lexicon = assertDoesNotThrow(repository::findAvailablePhoneLexiconForAiAdvisor);
        assertTrue(lexicon.stream().allMatch(row ->
                row.getProductName() != null && !row.getProductName().isBlank()
                        && row.getBrand() != null && !row.getBrand().isBlank()));
    }
}
