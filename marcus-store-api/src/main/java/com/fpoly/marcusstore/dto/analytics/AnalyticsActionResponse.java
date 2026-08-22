package com.fpoly.marcusstore.dto.analytics;

import java.time.LocalDateTime;

public record AnalyticsActionResponse(Long actionId, String title, String reason, String priority,
                String status, String ownerUsername, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
