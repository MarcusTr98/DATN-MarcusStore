package com.fpoly.marcusstore.dto.analytics;

import java.math.BigDecimal;

public record AnalyticsMetric(
                BigDecimal currentValue,
                BigDecimal previousValue,
                Double changePercent) {
}
