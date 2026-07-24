package com.fpoly.marcusstore.repository.statistics;

import java.math.BigDecimal;

public interface PaymentStatusProjection {
    String getPaymentMethod();
    String getOrderStatus();
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
}