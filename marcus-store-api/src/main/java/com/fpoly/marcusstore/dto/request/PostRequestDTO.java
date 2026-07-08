package com.fpoly.marcusstore.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;


@Data
public class PostRequestDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    private String thumbnailUrl;

    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự")
    private String excerpt;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private Boolean isPublished = false;

    @NotNull(message = "Danh mục không được để trống")
    private Integer postCategoryId;
    //cho người dúng chọn thời gian đăng bài trong tương lai
    private LocalDateTime publishedAt;
}
