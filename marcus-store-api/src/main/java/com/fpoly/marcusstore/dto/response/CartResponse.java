package com.fpoly.marcusstore.dto.response;

import com.fpoly.marcusstore.entity.shopping.CartItem;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponse {
    private Integer userId;
    private Integer cartId;
    private List<CartItemResponse> items;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
}
