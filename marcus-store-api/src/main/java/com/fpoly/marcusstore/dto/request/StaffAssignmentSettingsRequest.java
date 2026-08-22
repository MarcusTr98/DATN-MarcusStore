package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffAssignmentSettingsRequest {
    @NotNull
    private Boolean acceptingOrders;

    @NotNull
    @Min(1)
    @Max(50)
    private Integer maxActiveOrders;
}
