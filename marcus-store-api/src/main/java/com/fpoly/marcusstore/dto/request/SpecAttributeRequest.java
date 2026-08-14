package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class SpecAttributeRequest {

    @NotNull(message = "ID Danh mục không được để trống")
    @Positive(message = "ID Danh mục không hợp lệ")
    private Integer categoryId;

    @NotBlank(message = "Tên thông số không được để trống")
    @Size(max = 100, message = "Tên thông số tối đa 100 ký tự")
    private String name;

    @Size(max = 20, message = "Đơn vị tối đa 20 ký tự")
    private String unit;

    @NotBlank(message = "Kiểu dữ liệu không được để trống")
    @Pattern(regexp = "^(text|number|boolean)$", message = "Kiểu dữ liệu phải là text, number hoặc boolean")
    private String dataType = "text";

    @Positive(message = "Thứ tự hiển thị phải >= 0")
    private Integer displayOrder = 0;
}
