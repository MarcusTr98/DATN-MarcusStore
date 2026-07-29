package com.fpoly.marcusstore.dto.analytics;

public record AnalyticsRateMetric(
        double currentPercent,
        double previousPercent,
        double percentagePointChange) {
}
