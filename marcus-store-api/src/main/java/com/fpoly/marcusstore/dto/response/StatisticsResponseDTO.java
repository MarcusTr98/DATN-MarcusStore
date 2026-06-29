package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StatisticsResponseDTO {

    private String reportDate;
    private Integer reportYear;
    private Integer reportMonth;

    private Long totalOrders;
    private Long totalProductsSold;
    private BigDecimal totalRevenue;
}