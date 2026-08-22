package com.fpoly.marcusstore.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AnalyticsActionStatusRequest(
                @NotBlank @Pattern(regexp = "ACCEPTED|IN_PROGRESS|DONE|REJECTED") String status) {
}
