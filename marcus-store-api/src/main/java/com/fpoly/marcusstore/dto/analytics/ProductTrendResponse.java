package com.fpoly.marcusstore.dto.analytics;

import java.math.BigDecimal;

public record ProductTrendResponse(
        Integer productId,
        String productName,
        String brand,
        long currentUnits,
        long previousUnits,
        Double unitsChangePercent,
        BigDecimal currentMerchandiseSales,
        BigDecimal previousMerchandiseSales) {
}
