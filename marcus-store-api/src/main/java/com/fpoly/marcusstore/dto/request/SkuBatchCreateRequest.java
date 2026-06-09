package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class SkuBatchCreateRequest {

    @NotNull(message = "ID Sản phẩm không được để trống")
    private Integer productId;

    @NotEmpty(message = "Danh sách SKU không được để trống")
    private List<SkuItem> skus;

    @Getter
    @Setter
    public static class SkuItem {
        @NotBlank(message = "Mã SKU không được để trống")
        private String skuCode;

        @NotNull(message = "Giá không được để trống")
        @Min(value = 0, message = "Giá không được âm")
        private BigDecimal price;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        private Integer stock; // Đã đổi tên thành 'stock' khớp 100% với JSON của VueJS

        @NotEmpty(message = "Danh sách thuộc tính không được để trống")
        private List<Integer> valueIds;
    }
}