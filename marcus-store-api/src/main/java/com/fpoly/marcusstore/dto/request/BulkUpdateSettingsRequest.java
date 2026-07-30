package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class BulkUpdateSettingsRequest {
    @NotEmpty(message = "Danh sách cấu hình không được để trống")
    @Size(max = 30, message = "Không được cập nhật quá 30 cấu hình mỗi lần")
    private Map<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$", message = "Khóa cấu hình không hợp lệ") String, @Size(max = 20_000, message = "Giá trị cấu hình vượt quá giới hạn") String> settings;
}
