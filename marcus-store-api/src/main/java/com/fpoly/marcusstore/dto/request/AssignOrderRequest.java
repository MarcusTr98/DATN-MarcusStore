package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignOrderRequest {
    @NotNull(message = "Nhân viên phụ trách là bắt buộc")
    @Positive(message = "Mã nhân viên không hợp lệ")
    private Integer staffId;

    @Size(max = 500, message = "Lý do phân công không được vượt quá 500 ký tự")
    private String reason;
}
