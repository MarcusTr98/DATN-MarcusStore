package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemDetailResponse {
    private Integer orderItemId;
    private Integer skuId;
    private String skuCode;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal lineTotal;
    private List<ImeiResponse> imeis;
    private Boolean isFlashSale;
    private BigDecimal originalPrice;
    private String flashSaleSlotName;
    private List<ClientSkuAttributeValueResponse> variants;
    private Boolean reviewed;
}
