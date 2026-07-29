package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReplyRequest {

    @NotBlank(message = "Nội dung không được để trống")
    private String replyText;

}