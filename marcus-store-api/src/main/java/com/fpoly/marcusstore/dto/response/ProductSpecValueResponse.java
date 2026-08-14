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
public class ProductSpecValueResponse {
    private Integer id;
    private Integer productId;
    private Integer specAttributeId;
    private String specAttributeName;
    private String unit;
    private String dataType;
    private Integer displayOrder;
    private String valueText;
}
