package com.fpoly.marcusstore.dto.analytics;

public record AnalyticsOverviewResponse(
        AnalyticsPeriod period,
        AnalyticsMetric completedSales,
        AnalyticsMetric completedOrders,
        AnalyticsMetric unitsSold,
        AnalyticsMetric averageOrderValue,
        AnalyticsRateMetric completionRate,
        AnalyticsRateMetric cancellationRate,
        AnalyticsMetric successfulRefundAmount,
        AnalyticsMetric orderingCustomers) {
}
