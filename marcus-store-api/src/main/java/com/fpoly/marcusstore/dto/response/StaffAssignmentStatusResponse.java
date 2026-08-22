package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class StaffAssignmentStatusResponse {
    private final boolean acceptingOrders;
    private final int maxActiveOrders;
    private final long activeOrderCount;
    private final long workloadScore;
    private final Map<String, Long> workloadBreakdown;
    private final boolean canClaim;
    private final LocalDateTime lastAssignedAt;
    private final String unavailableReason;
    private final long pendingOrderCount;
    private final long cooldownRemainingSeconds;
    private final long assignedInPeriodCount;
    private final long selfAssignedInPeriodCount;
    private final double selfAssignmentRate;
    private final long completedInPeriodCount;
    private final double periodCompletionRate;
}
