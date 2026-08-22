package com.fpoly.marcusstore.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalyticsActionRequest(
                @NotBlank @Size(max = 180) String title,
                @NotBlank @Size(max = 300) String reason,
                @NotBlank @Pattern(regexp = "HIGH|MEDIUM|LOW") String priority) {
}
