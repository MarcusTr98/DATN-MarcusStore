package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
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
public class ProductImgRequest {

    private String imageUrl;
    
    private Boolean isPrimary;
    
    @Min(value = 0, message = "Thứ tự không được nhỏ hơn 0")
    private Integer displayOrder;
}
