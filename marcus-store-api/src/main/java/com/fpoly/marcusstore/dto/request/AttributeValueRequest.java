package com.fpoly.marcusstore.dto.request;

import lombok.Data;

@Data
public class AttributeValueRequest {
    private Integer attributeId;
    private String valueString;
}