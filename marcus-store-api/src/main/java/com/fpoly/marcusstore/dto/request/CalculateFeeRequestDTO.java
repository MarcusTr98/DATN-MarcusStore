package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalculateFeeRequestDTO {
    @NotNull(message = "Thiếu District ID của GHN")
    private Integer toDistrictId;

    @NotBlank(message = "Thiếu Ward Code của GHN")
    private String toWardCode;
}