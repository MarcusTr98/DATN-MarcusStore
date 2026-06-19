package com.fpoly.marcusstore.repository.statistics;

public interface OrderByWeekdayProjection {
    Integer getDayOfWeek();
    Long getTotalOrders();
}