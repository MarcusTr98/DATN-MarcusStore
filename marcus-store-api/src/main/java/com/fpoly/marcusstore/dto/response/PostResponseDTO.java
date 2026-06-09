package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

//DTO trả dữ liệu về cho client
 
@Data
@Builder
public class PostResponseDTO {

    private Integer id;
    private String title;
    private String slug;
    private String thumbnailUrl;
    private String excerpt;
    private String content;
    private Boolean isPublished;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Thông tin danh mục
    private Integer postCategoryId;
    private String postCategoryName;
    private String postCategorySlug;

    // Thông tin tác giả
    private Integer authorId;
    private String authorName;
}