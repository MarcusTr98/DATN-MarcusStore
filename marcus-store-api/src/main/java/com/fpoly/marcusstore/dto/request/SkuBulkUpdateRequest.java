package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class SkuBulkUpdateRequest {

    @NotEmpty(message = "Danh sách SKU không được để trống")
    @Size(max = 200, message = "Chỉ được cập nhật tối đa 200 SKU mỗi lần")
    private List<@Valid SkuUpdateItem> skus;

    @Getter
    @Setter
    public static class SkuUpdateItem {
        @NotNull(message = "ID SKU không được để trống")
        @Positive(message = "ID SKU không hợp lệ")
        private Integer skuId;
        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2, message = "Giá không hợp lệ")
        private BigDecimal price;

        @NotNull(message = "Giá niêm yết không được để trống")
        @DecimalMin(value = "0.01", message = "Giá niêm yết phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2, message = "Giá niêm yết không hợp lệ")
        private BigDecimal originalPrice;

        // Marcus thêm: bulk SKU chỉ quản lý giá; tồn kho thuộc module kho/IMEI để
        // tránh một request giá cũ ghi đè số lượng thực tế.
        @AssertTrue(message = "Giá bán không được lớn hơn giá niêm yết")
        public boolean isPriceRangeValid() {
            return price == null || originalPrice == null || price.compareTo(originalPrice) <= 0;
        }
    }

}
