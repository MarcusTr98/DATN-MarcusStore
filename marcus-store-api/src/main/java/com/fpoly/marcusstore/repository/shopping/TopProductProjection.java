package com.fpoly.marcusstore.repository.shopping;

import java.math.BigDecimal;


public interface TopProductProjection {
    String getProductName();
    Long getTotalSold();
    BigDecimal getRevenue();
}