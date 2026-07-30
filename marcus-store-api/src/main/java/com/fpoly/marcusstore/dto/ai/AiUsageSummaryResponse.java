package com.fpoly.marcusstore.dto.ai;

public record AiUsageSummaryResponse(
        long successfulChats,
        long failedChats,
        long productClicks,
        long uniqueSessions,
        double successRate,
        double clickThroughRate,
        long averageResponseTimeMs) {
}
