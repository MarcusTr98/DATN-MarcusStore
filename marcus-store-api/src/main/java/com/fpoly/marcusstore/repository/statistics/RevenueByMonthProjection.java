package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;


public interface RevenueByMonthProjection {
    Integer getReportYear();
    Integer getReportMonth();
    Long getTotalOrders();
    Long getTotalProductsSold();
    BigDecimal getTotalRevenue();
}