package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CalculateFeeRequestDTO {
    @NotNull(message = "Thiếu District ID của GHN")
    @Positive(message = "District ID của GHN không hợp lệ")
    private Integer toDistrictId;

    @NotBlank(message = "Thiếu Ward Code của GHN")
    @Size(max = 20, message = "Ward Code của GHN không hợp lệ")
    private String toWardCode;
}
