package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateCategory {
    @NotBlank(message = "Tên danh mục không được để trống")
    private String categoryName;

    private Boolean status;
    
    private Integer parentId;
}
