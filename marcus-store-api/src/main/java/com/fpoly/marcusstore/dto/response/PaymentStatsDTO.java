package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PaymentStatsDTO {

    private List<MethodSlice> byMethod;
    private List<StatusSlice> byStatus;

    @Data
    @Builder
    public static class MethodSlice {
        private String method;
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private double percentage;
    }

    @Data
    @Builder
    public static class StatusSlice {
        private String status;
        private Long totalOrders;
        private double percentage;
    }
}