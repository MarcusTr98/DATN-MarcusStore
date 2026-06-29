package com.fpoly.marcusstore.repository.shopping;
import java.math.BigDecimal;

public interface TopCustomerProjection {
    String getCustomerName();
    String getEmail();
    Long getTotalOrders();
    BigDecimal getTotalSpent();
}