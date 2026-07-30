package com.fpoly.marcusstore.dto.analytics;

import java.time.LocalDateTime;
import java.util.List;

public record AiAnalyticsReportResponse(
                LocalDateTime generatedAt,
                LocalDateTime cachedUntil,
                boolean cached,
                String headline,
                String executiveSummary,
                String outlook,
                String confidence,
                List<Signal> signals,
                List<Action> actions,
                List<ProductOutlook> productOutlooks,
                String disclaimer) {

        public AiAnalyticsReportResponse asCached() {
                return new AiAnalyticsReportResponse(
                                generatedAt,
                                cachedUntil,
                                true,
                                headline,
                                executiveSummary,
                                outlook,
                                confidence,
                                signals,
                                actions,
                                productOutlooks,
                                disclaimer);
        }

        public record Signal(
                        String title,
                        String evidence,
                        String interpretation,
                        String severity) {
        }

        public record Action(
                        String title,
                        String reason,
                        String priority) {
        }

        public record ProductOutlook(
                        Integer productId,
                        String productName,
                        String direction,
                        String reason) {
        }
}
