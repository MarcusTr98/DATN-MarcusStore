package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderAssignmentDashboardResponse {
    private final List<StaffLoad> staffLoads;
    private final List<PendingOrder> pendingOrders;

    @Getter
    @Builder
    public static class StaffLoad {
        private final Integer staffId;
        private final String staffName;
        private final long activeOrderCount;
        private final double workloadRate;
    }

    @Getter
    @Builder
    public static class PendingOrder {
        private final String orderCode;
        private final String recipientName;
        private final BigDecimal finalAmount;
        private final LocalDateTime autoAssignAt;
        private final Integer plannedStaffId;
        private final String plannedStaffName;
    }
}
