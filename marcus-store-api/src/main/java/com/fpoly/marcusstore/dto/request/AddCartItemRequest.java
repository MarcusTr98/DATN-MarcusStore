package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddCartItemRequest {
    private Integer skuId;
    private Integer quantity;
    // ID của FlashSaleSlot - nếu user mua từ trang Flash Sale
    // Nếu NULL hoặc không có → coi như sản phẩm bình thường
    private Long flashSaleSlotId;
    // Giá Flash Sale đã được chốt tại thời điểm user thêm vào giỏ
    // Lưu lại để tránh trường hợp giá thay đổi sau đó
    private BigDecimal flashSalePrice;
}
