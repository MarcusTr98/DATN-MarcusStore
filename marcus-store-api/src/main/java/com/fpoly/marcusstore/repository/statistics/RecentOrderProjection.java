package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;
import java.sql.Timestamp;

public interface RecentOrderProjection {
    String getOrderCode();
    String getCustomerName();
    String getPhone();
    String getPaymentMethod();
    String getOrderStatus();
    BigDecimal getTotalAmount();
    Timestamp getCreatedAt();
}