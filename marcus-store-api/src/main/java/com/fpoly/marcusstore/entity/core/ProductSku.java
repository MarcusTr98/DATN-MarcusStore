package com.fpoly.marcusstore.entity.core;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Product_Skus")
@Getter
@Setter
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sku_id")
    private Integer skuId;

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "sku_image_url", length = 500)
    private String skuImageUrl;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Trỏ về Product cha
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference // Nhận tín hiệu từ @JsonManagedReference của Product
    private Product product;

    // TỐI ƯU 3: Bảng trung gian Sku_Attribute_Values xử lý bằng @ManyToMany
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "Sku_Attribute_Values", joinColumns = @JoinColumn(name = "sku_id"), inverseJoinColumns = @JoinColumn(name = "value_id"))
    private List<AttributeValue> attributeValues = new ArrayList<>();
}