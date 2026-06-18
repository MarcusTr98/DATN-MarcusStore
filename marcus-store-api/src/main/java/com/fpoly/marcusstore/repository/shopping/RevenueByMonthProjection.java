package com.fpoly.marcusstore.repository.shopping;

import java.math.BigDecimal;


public interface RevenueByMonthProjection {
    Integer getReportYear();
    Integer getReportMonth();
    Long getTotalOrders();
    Long getTotalProductsSold();
    BigDecimal getTotalRevenue();
}