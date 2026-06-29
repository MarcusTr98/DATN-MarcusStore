package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class KpiSummaryDTO {
    private BigDecimal totalRevenue;
    private Long       totalOrders;
    private Long       totalProductsSold;
}