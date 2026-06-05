package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DeleteSelectedCartItemRequest {
    private List<Integer> skuIds;
}
