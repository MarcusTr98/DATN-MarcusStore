package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductCascadeResponse {
    // lấy mục cha lớn nhất nhãn hàng: iphone, samsung, xiaomi....
    private String brand;
    // lấy mục cha lớn thứ 2 : iphone: iphone 14, iphon 15.....
    private List<CategoryL2Node> categories;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryL2Node {
        private Integer categoryId;
        private String categoryName;
        private List<SkuNode> skus;
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuNode {
        private Integer skuId;
        private String productName;     // Tên sản phẩm : iphong 15 promax
        private String skuCode;        // Mã SKU: IP15PM-BLK-1TB
        private BigDecimal originalPrice;
        private Integer stockQuantity;
        private String attributes;     //256GB, Đen - gộp các biến thể của một sku lại
    }
}
