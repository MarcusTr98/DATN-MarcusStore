package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CartItemResponse {
    private Integer cartItemId;
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
    private Integer stockQuantity;
    private String color;
    private String storage;
    // lưu chuỗi hiển thị của màu và dung lượng
    private String variantText;

}
