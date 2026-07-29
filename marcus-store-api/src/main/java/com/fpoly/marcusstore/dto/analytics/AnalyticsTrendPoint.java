package com.fpoly.marcusstore.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnalyticsTrendPoint(
        LocalDate date,
        BigDecimal completedSales,
        long completedOrders,
        long unitsSold) {
}
