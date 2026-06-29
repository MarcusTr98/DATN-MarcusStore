package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ShippingCalculateRequest {

    @NotNull(message = "ID Quận/Huyện không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Mã Phường/Xã không được để trống")
    private String toWardCode;

    @NotNull(message = "Tổng khối lượng không được để trống")
    @Min(value = 1, message = "Khối lượng phải lớn hơn 0")
    private Integer totalWeightGram;

    @NotNull(message = "Tổng giá trị đơn hàng không được để trống")
    @Min(value = 0, message = "Tổng giá trị đơn hàng không hợp lệ")
    private BigDecimal cartTotal;
}