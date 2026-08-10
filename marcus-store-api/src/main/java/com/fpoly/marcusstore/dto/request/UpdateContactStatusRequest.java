package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateContactStatusRequest {
    @NotBlank(message = "Trạng thái liên hệ không được để trống")
    @Pattern(regexp = "^(NEW|IN_PROGRESS|RESOLVED|SPAM)$", message = "Trạng thái liên hệ không hợp lệ")
    private String status;
}
