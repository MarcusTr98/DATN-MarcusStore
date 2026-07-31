package com.fpoly.marcusstore.dto.analytics;

public record CancellationReasonResponse(
                String reason,
                long currentCount,
                long previousCount,
                double sharePercent,
                Double changePercent) {
}
