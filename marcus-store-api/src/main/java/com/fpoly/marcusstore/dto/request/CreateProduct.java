package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateProduct {
    @NotBlank(message = "Ko được để trống tên sản phẩm")
    private String productName;

    @NotBlank(message = "Ko được để trống mô tả sản phẩm")
    private String description;

    @NotBlank(message = "Ko được để trống thương hiệu sản phẩm")
    private String brand;

    // Marcus thêm sau khi tích hợp module kho: chọn nguồn quản lý tồn ngay khi
    // tạo Product, trước bước sinh SKU/nhập hàng.
    @NotNull(message = "Vui lòng chọn cách quản lý tồn kho")
    private Boolean statusImei;

    private String thumbnailUrl;

    @NotNull(message = "Ko được để trống CategoryId")
    private Integer categoryId;
}
