package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttributeRequest {
    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 50, message = "Tên thuộc tính không được vượt quá 50 ký tự")
    @Pattern(regexp = "^[\\p{L}\\p{N} ._\\-/]+$", message = "Tên thuộc tính chứa ký tự không hợp lệ")
    private String name;
}
