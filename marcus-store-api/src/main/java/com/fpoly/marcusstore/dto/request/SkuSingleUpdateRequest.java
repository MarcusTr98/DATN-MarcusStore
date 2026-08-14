package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @NotNull(message = "Giá niêm yết không được để trống")
    @DecimalMin(value = "0.01", message = "Giá niêm yết phải lớn hơn 0")
    @Digits(integer = 13, fraction = 2, message = "Giá gốc không hợp lệ")
    private BigDecimal originalPrice;

    // Marcus thêm: Admin sửa khối lượng đóng gói tại SKU để Checkout và GHN
    // dùng cùng một nguồn dữ liệu.
    @NotNull(message = "Khối lượng SKU không được để trống")
    @Min(value = 1, message = "Khối lượng SKU phải lớn hơn 0 gram")
    @Max(value = 50000, message = "Khối lượng SKU không được vượt quá 50.000 gram")
    private Integer weightGram;

    // Marcus thêm: giá bán thường không được cao hơn giá niêm yết. Flash Sale có
    // bảng giá riêng nên không gửi giá Flash Sale qua DTO này.
    @AssertTrue(message = "Giá bán không được lớn hơn giá niêm yết")
    public boolean isPriceRangeValid() {
        return price == null || originalPrice == null || price.compareTo(originalPrice) <= 0;
    }
}
