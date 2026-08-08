package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemUpdateRequest {

    @NotBlank(message = "IMEI không được để trống")
    @Size(max = 50)
    private String imeiCode;

    private Integer status;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    @NotBlank(message = "Note không được bỏ trống")
    private String note;
}
