package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShippingFeeRequest {
    @NotNull(message = "Thiếu ID Quận/Huyện")
    private Integer toDistrictId;

    @NotBlank(message = "Thiếu mã Phường/Xã")
    private String toWardCode;
}