package com.fpoly.marcusstore.dto.response;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReplyResponse {

    private Integer replyId;

    private Integer staffId;

    private String staffName;

    private String replyText;

    private LocalDateTime createdAt;
}