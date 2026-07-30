package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminAvailabilityRequest {
    @NotNull(message = "Trạng thái trực tuyến không được để trống")
    private Boolean available;
}
