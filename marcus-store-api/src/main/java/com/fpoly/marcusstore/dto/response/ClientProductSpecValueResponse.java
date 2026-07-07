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
public class ClientProductSpecValueResponse {

    private Integer specAttributeId;
    private String specAttributeName;
    private String unit;
    private String dataType;
    private String valueText;
}
