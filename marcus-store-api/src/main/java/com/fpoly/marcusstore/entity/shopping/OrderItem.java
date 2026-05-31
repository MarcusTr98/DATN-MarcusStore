package com.fpoly.marcusstore.entity.shopping;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.core.ProductItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Order_Items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Integer orderItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Snapshot: Lưu giá bán ngay tại thời điểm khách click Mua
    @Column(name = "price_at_purchase", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceAtPurchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    @JsonIgnore
    private ProductSku sku;

    // CHÚ Ý: Móc nối với bảng IMEI (Quản lý thiết bị vật lý xuất kho)
    @OneToMany(mappedBy = "orderItem")
    @JsonManagedReference
    private List<ProductItem> productItems = new ArrayList<>();
}