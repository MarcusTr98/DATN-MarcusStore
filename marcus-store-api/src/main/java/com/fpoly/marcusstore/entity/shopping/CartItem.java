package com.fpoly.marcusstore.entity.shopping;

import com.fpoly.marcusstore.entity.core.ProductSku;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Cart_Items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer cartItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    private ProductSku productSku;

    @Column(nullable = false)
    private Integer quantity;
}