package com.fpoly.marcusstore.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewAdminResponse {

    private Integer reviewId;

    private Integer userId;
    private String username;
    private String fullName;

    private Integer productId;
    private String productName;

    private Integer rating;

    private String commentText;

    private LocalDateTime createdAt;

    private Integer orderItemId;

    private ReviewReplyResponse reply;

    private List<String> images;
}