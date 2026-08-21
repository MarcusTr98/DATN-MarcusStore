package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StaffAssignmentStatusResponse {
    private final boolean acceptingOrders;
    private final int maxActiveOrders;
    private final long activeOrderCount;
    private final double workloadScore;
    private final boolean canClaim;
    private final LocalDateTime lastAssignedAt;
    private final String unavailableReason;
}
