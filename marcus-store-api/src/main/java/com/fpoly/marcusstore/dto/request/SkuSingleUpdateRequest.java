package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SkuSingleUpdateRequest {

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    @Digits(integer = 13, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal price;

    @DecimalMin(value = "0.00", message = "Giá gốc không được âm")
    @Digits(integer = 13, fraction = 2, message = "Giá gốc không hợp lệ")
    private BigDecimal originalPrice;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    @Max(value = 1_000_000, message = "Số lượng vượt quá giới hạn")
    private Integer stockQuantity;
}
