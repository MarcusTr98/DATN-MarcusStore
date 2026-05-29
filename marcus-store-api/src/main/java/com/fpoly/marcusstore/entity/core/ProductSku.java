package com.fpoly.marcusstore.entity.core;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "Product_Skus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Integer skuId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "sku_image_url", length = 500)
    private String skuImageUrl;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "Sku_Attribute_Values", joinColumns = @JoinColumn(name = "sku_id"), inverseJoinColumns = @JoinColumn(name = "value_id"))
    private Set<AttributeValue> attributeValues = new HashSet<>();
}