package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;

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
}
