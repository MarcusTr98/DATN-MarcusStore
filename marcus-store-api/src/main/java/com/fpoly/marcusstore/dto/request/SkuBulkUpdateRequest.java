package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SkuBulkUpdateRequest {

    private List<SkuUpdateItem> skus;

    @Getter
    @Setter
    public static class SkuUpdateItem {
        private Integer skuId;
        private BigDecimal price;
        private Integer stockQuantity;
    }
}