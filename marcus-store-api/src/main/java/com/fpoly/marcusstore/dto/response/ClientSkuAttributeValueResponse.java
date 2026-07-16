package com.fpoly.marcusstore.dto.response;

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
public class ClientSkuAttributeValueResponse {

    private Integer valueId;
    private Integer attributeId;
    private String attributeName;
    private String valueString;
    private String valueMeta;
}