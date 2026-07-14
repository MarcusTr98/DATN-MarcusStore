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
    private String color;
    private String storage;
    // lưu chuỗi hiển thị của màu và dung lượng
    private String variantText;

    // Đánh dấu sản phẩm có phải Flash Sale không
    private Boolean isFlashSale;

    // Giá gốc trước khi giảm (để hiển thị % giảm giá)
    private BigDecimal originalPrice;

    // Tên slot Flash Sale (nếu có)
    private String flashSaleSlotName;
}
