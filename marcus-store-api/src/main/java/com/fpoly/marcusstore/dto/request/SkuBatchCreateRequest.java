package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SkuBatchCreateRequest {

    @NotNull(message = "ID Sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm không hợp lệ")
    private Integer productId;

    @NotEmpty(message = "Danh sách SKU không được để trống")
    @Size(max = 200, message = "Chỉ được tạo tối đa 200 SKU mỗi lần")
    private List<@Valid SkuItem> skus;

    @Getter
    @Setter
    public static class SkuItem {
        @NotBlank(message = "Mã SKU không được để trống")
        @Size(max = 50, message = "Mã SKU không được vượt quá 50 ký tự")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Mã SKU sai định dạng")
        private String skuCode;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2, message = "Giá không hợp lệ")
        private BigDecimal price;

        @DecimalMin(value = "0.01", message = "Giá niêm yết phải lớn hơn 0")
        @Digits(integer = 13, fraction = 2, message = "Giá niêm yết không hợp lệ")
        private BigDecimal originalPrice;

        @NotEmpty(message = "Danh sách thuộc tính không được để trống")
        @Size(max = 30, message = "SKU có quá nhiều thuộc tính")
        private List<@Positive(message = "ID giá trị thuộc tính không hợp lệ") Integer> valueIds;

        // Marcus thêm: kiểm tra ngay tại DTO để frontend nhận đúng lỗi từng dòng,
        // service vẫn kiểm tra lại trước khi ghi cả batch.
        @AssertTrue(message = "Giá bán không được lớn hơn giá niêm yết")
        public boolean isPriceRangeValid() {
            return price == null || originalPrice == null || price.compareTo(originalPrice) <= 0;
        }
    }
}
