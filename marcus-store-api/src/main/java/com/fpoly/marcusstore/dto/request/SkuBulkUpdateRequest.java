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
        @NotNull(message = "Tồn kho không được để trống")
        @Min(value = 0, message = "Tồn kho không được âm")
        @Max(value = 1_000_000, message = "Tồn kho vượt quá giới hạn")
        private Integer stockQuantity;
    }

}
