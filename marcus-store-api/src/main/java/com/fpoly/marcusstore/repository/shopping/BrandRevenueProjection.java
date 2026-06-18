package com.fpoly.marcusstore.repository.shopping;

import java.math.BigDecimal;

public interface BrandRevenueProjection {
    String getBrand();
    Long getTotalSold();
    BigDecimal getRevenue();
}