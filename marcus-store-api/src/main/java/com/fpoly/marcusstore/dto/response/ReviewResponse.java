package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ReviewResponse {

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

     // Ảnh đánh giá
    private List<String> images;

    // ===== Phản hồi của cửa hàng =====

    private String replyContent;

    private String replyStaffName;

    private LocalDateTime replyCreatedAt;
}