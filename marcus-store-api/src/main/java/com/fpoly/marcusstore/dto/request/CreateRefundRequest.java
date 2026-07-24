package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateRefundRequest {
    @NotBlank(message = "Lý do hoàn tiền không được để trống")
    @Size(max = 500, message = "Lý do hoàn tiền không được vượt quá 500 ký tự")
    private String reason;
}
