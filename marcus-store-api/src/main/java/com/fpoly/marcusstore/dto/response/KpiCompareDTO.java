package com.fpoly.marcusstore.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiCompareDTO {

    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long completedOrders;
    private Long totalProductsSold;

    private Double revenueChangePercent;
    private Double ordersChangePercent;
    private Double completedOrdersChangePercent;
    private Double productsSoldChangePercent;

    private String previousLabel;
}