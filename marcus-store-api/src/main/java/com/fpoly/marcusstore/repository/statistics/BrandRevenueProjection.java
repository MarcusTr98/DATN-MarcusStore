package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;

public interface BrandRevenueProjection {
    String getBrand();
    Long getTotalSold();
    BigDecimal getRevenue();
}