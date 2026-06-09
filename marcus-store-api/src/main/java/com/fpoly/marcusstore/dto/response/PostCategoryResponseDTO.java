package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

// DTO trả dữ liệu về cho client Không lộ danh sách posts bên trong
 
@Data
@Builder
public class PostCategoryResponseDTO {

    private Integer id;
    private String name;
    private String slug;
    private Boolean status;
}