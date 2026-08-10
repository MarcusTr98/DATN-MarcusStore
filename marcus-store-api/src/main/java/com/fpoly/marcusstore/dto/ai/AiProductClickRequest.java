package com.fpoly.marcusstore.dto.ai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AiProductClickRequest {

    @NotNull
    @Positive
    private Integer productId;

    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$", message = "Mã phiên theo dõi không hợp lệ.")
    private String sessionId;

    // Marcus thêm: liên kết click với đúng câu tư vấn, không lưu nội dung chat.
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Mã câu tư vấn không hợp lệ.")
    private String adviceId;
}
