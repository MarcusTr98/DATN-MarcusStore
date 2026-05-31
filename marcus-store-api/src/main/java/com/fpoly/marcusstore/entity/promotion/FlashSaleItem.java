package com.fpoly.marcusstore.entity.promotion;

import com.fpoly.marcusstore.entity.core.ProductSku;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Flash_Sale_Items")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class FlashSaleItem {

    @EmbeddedId
    private FlashSaleItemId id;

    @Column(name = "original_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "flash_sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal flashSalePrice;

    @Column(name = "flash_sale_quantity", nullable = false)
    private Integer flashSaleQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Khai báo lại quan hệ với các bảng Cha (Bắt buộc thêm insertable = false,
    // updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", insertable = false, updatable = false)
    private FlashSaleSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sku_id", insertable = false, updatable = false)
    private ProductSku sku;
}