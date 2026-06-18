package com.fpoly.marcusstore.repository.shopping;

import java.math.BigDecimal;
import java.sql.Date;

public interface RevenueByDayProjection {
    Date getReportDate();
    Long getTotalOrders();
    Long getTotalProductsSold();
    BigDecimal getTotalRevenue();
}