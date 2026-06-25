package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;

public interface KpiSummaryProjection {
    BigDecimal getTotalRevenue();
    Long getTotalOrders();
    Long getTotalProductsSold();
}