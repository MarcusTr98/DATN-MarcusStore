package com.fpoly.marcusstore.entity.core;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "Product_Items")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ProductItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "imei_code", nullable = false, unique = true, length = 50)
    private String imeiCode;

    @Column(name = "status")
    private Integer status = 1; // 1: Trong kho, 2: Đã bán, 3: Bảo hành

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", nullable = false)
    @JsonBackReference
    private ProductSku productSku;

    // Liên kết trực tiếp sang dòng chi tiết đơn hàng thuộc package shopping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    @JsonBackReference
    private OrderItem orderItem;
}