package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffAvailabilityRequest {
    @NotNull(message = "Trạng thái nhận đơn là bắt buộc")
    private Boolean acceptingOrders;
}
