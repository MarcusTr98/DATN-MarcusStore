package com.fpoly.marcusstore.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiAdvisorRequest {

    @NotBlank(message = "Vui lòng nhập câu hỏi.")
    @Size(max = 500, message = "Câu hỏi không được vượt quá 500 ký tự.")
    private String message;

    @Valid
    @Size(max = 6, message = "Chỉ gửi tối đa 6 tin nhắn gần nhất.")
    private List<ConversationTurn> history = new ArrayList<>();

    @Data
    public static class ConversationTurn {
        @NotBlank
        @Pattern(regexp = "user|assistant", message = "Vai trò hội thoại không hợp lệ.")
        private String role;

        @NotBlank
        @Size(max = 500)
        private String content;
    }
}
