package com.fpoly.marcusstore.repository.shopping;

public interface OrderByWeekdayProjection {
    Integer getDayOfWeek();
    Long getTotalOrders();
}