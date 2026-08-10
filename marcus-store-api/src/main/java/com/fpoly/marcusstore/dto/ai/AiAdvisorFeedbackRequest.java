package com.fpoly.marcusstore.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AiAdvisorFeedbackRequest {
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Mã câu trả lời không hợp lệ")
    private String adviceId;
    @NotBlank
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "Mã phiên không hợp lệ")
    private String sessionId;
    @NotNull
    private Boolean helpful;
}
