package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

 //DTO nhận dữ liệu từ client khi thêm / sửa bannerCó
 // @Valid để Spring tự động kiểm tra dữ liệu đầu vào
 
@Data
public class BannerRequestDTO {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "URL ảnh không được để trống")
    private String imageUrl;
    private String targetUrl;

    @Min(value = 0, message = "Thứ tự hiển thị phải >= 0")
    private Integer displayOrder = 0;

    private Boolean isActive = true;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull(message = "Vị trí banner không được để trống")
    private Integer positionId;
}