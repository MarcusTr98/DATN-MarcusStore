package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class OrderAssignmentDashboardResponse {
    private final List<StaffLoad> staffLoads;
    private final List<PendingOrder> pendingOrders;
    private final long pendingTotalElements;
    private final int pendingTotalPages;
    private final int pendingPage;

    @Getter
    @Builder
    public static class StaffLoad {
        private final Integer staffId;
        private final String staffName;
        private final long activeOrderCount;
        private final double workloadRate;
        private final long completedOrderCount;
        private final double completionRate;
        private final long workloadScore;
        private final boolean acceptingOrders;
        private final int maxActiveOrders;
        private final boolean eligibleForAssignment;
        private final Map<String, Long> workloadBreakdown;
        private final long selfAssignedCount;
        private final long autoAssignedCount;
        private final long manualAssignedCount;
        private final long totalAssignedCount;
        private final double selfAssignmentRate;
        private final long completedInPeriodCount;
        private final double periodCompletionRate;
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
