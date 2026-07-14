package com.fpoly.marcusstore.entity.shopping;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Cart_Items")
@Getter
@Setter
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonBackReference
    private Cart cart;

    // Trỏ về mã SKU cụ thể (VD: iPhone Đỏ 256GB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku sku;

    // Lưu giá Flash Sale khi mua từ trang Flash Sale
    // NULL = sản phẩm bình thường (dùng sku.getPrice())
    @Column(name = "flash_sale_price", precision = 18, scale = 2)
    private BigDecimal flashSalePrice;


    // Tham chiếu đến FlashSaleSlot để biết sản phẩm thuộc khung giờ nào
    // NULL = không phải sản phẩm Flash Sale
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_sale_slot_id")
    private FlashSaleSlot flashSaleSlot;
}
