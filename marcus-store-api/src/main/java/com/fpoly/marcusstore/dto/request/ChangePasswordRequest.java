package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message="Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;
    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    private String newPassword;
    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;
}
