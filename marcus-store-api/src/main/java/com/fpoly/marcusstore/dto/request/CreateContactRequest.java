package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateContactRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    private String email;

    @NotBlank(message = "Nội dung không được để trống")
    private String message;
}