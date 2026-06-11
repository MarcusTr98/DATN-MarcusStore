package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 6)
    private String password;

    @Email
    private String email;

    @NotBlank
    private String fullName;

    @Pattern(
        regexp = "^0\\d{9}$",
        message = "Số điện thoại không hợp lệ"
    )
    private String phoneNumber;
}
