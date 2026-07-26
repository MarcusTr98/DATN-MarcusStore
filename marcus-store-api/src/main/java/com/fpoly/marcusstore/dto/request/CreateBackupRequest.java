package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBackupRequest {
    @NotBlank(message = "Vui lòng chọn loại sao lưu")
    @Pattern(regexp = "EXCEL|BAK", message = "Loại sao lưu chỉ nhận EXCEL hoặc BAK")
    private String type;

    @Size(max = 250, message = "Ghi chú không được vượt quá 250 ký tự")
    private String note;
}
