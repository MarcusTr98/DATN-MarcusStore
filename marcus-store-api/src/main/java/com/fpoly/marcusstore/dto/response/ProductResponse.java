package com.fpoly.marcusstore.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductResponse {
    private Integer productId;
    private String productName;
    private String description;
    private String brand;
    private String thumbnailUrl;
    private Boolean status;
    private String slug;
    private LocalDateTime createdAt;
    private String categoryName;     
}
