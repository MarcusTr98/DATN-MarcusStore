package com.fpoly.marcusstore.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecsSaveRequest {

    @NotNull(message = "ID Sản phẩm không được để trống")
    @Positive(message = "ID sản phẩm không hợp lệ")
    private Integer productId;

    @Size(max = 200, message = "Mỗi sản phẩm chỉ được có tối đa 200 thông số")
    private List<@Valid SpecValueItem> specs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecValueItem {
        /** Null = tạo mới; có giá trị = cập nhật. */
        private Integer id;

        @NotNull(message = "ID thuộc tính thông số không được để trống")
        @Positive(message = "ID thuộc tính thông số không hợp lệ")
        private Integer specAttributeId;

        private String valueText;
    }
}
