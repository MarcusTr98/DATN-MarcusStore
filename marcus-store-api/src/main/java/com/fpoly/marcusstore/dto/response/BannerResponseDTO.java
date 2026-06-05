package com.fpoly.marcusstore.dto.response;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

 //DTO trả dữ liệu về cho client
@Data
@Builder
public class BannerResponseDTO {

    private Integer id;
    private String title;
    private String imageUrl;
    private String targetUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Thông tin vị trí banner
    private Integer positionId;
    private String positionCode;
    private String positionDescription;
}