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
public class CreateCategory {
    @NotBlank (message = "Không được để trống tên category")
    private String categoryName;

    private Integer parentId;
    
    private String slug;
}
