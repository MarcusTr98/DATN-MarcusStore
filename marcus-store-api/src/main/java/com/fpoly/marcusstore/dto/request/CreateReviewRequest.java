package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateReviewRequest {

    private Integer orderItemId;

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String commentText;

    // Danh sách URL ảnh sau khi upload Cloudinary
    private List<String> imageUrls;

}