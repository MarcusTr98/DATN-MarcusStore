package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecAttributeResponse {
    private Integer specAttributeId;
    private Integer categoryId;
    private String categoryName;
    private String name;
    private String unit;
    private String dataType;
    private Integer displayOrder;
}
