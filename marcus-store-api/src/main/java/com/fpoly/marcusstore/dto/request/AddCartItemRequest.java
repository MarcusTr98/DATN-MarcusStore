package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {
    private Integer skuId;
    private Integer quantity;
}
