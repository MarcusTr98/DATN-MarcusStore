package com.fpoly.marcusstore.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record SystemSettingsAdminResponse(
        Map<String, String> settings,
        String updatedBy,
        LocalDateTime updatedAt) {
}
