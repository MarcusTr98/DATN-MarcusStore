package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Marcus thêm: DTO nhỏ cho API ảnh SKU, tránh serialize Entity và collection
// attributeValues LAZY sau khi transaction đã đóng.
@Getter
@AllArgsConstructor
public class SkuImageUpdateResponse {
    private Integer skuId;
    private String skuImageUrl;
}
