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
public class UpdateProduct {
    @NotBlank(message = "Ko được để trống tên sản phẩm")
    private String productName;

    @NotBlank(message = "Ko được để trống mô tả sản phẩm")
    private String description;

    @NotBlank(message = "Ko được để trống thương hiệu sản phẩm")
    private String brand;

    @NotBlank(message = "Ko được để trống đường dẫn ảnh")
    private String thumbnailUrl;

    private Boolean status;

    @NotNull(message = "Ko được để trống CategoryId")
    private Integer categoryId;
}
