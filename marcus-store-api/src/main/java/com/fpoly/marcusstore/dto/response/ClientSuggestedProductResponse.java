package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientSuggestedProductResponse {
    private Integer productId;
    private String productName;
    private String slug;
    private String thumbnailUrl;
    private String brand;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Boolean inStock;
    private Double rating;
    private Long reviewCount;
    /**
     * SKU mặc định để thêm nhanh vào giỏ hàng từ card.
     * Quy tắc chọn: SKU active + còn hàng + giá thấp nhất.
     * Có thể null nếu sản phẩm chưa có SKU active/còn hàng
     * — FE lúc đó phải điều hướng sang trang chi tiết.
     */
    private Integer defaultSkuId;
}
