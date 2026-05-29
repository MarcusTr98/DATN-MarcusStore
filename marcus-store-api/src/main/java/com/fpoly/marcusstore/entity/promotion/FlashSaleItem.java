package com.fpoly.marcusstore.entity.promotion;

import com.fpoly.marcusstore.entity.core.ProductSku;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Flash_Sale_Items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleItem {

    @EmbeddedId
    private FlashSaleItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("slotId") // Map field slotId trong EmbeddedId với Entity này
    @JoinColumn(name = "slot_id")
    private FlashSaleSlot flashSaleSlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skuId") // Map field skuId trong EmbeddedId với Entity này
    @JoinColumn(name = "sku_id")
    private ProductSku productSku;

    @Column(name = "original_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "flash_sale_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal flashSalePrice;

    @Column(name = "flash_sale_quantity", nullable = false)
    private Integer flashSaleQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}