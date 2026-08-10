package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderImeiRequest {

    @NotNull(message = "orderItemId không được để trống")
    private Integer orderItemId;

    @NotEmpty(message = "Danh sách IMEI không được để trống")
    private List<String> imeiCodes;
}
