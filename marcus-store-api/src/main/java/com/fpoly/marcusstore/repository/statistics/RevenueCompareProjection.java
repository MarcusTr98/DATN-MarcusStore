package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;

public interface RevenueCompareProjection {
    Object getDateLabel();
    BigDecimal getTotalRevenue();
}