package com.fpoly.marcusstore.entity.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleItemId implements Serializable {
    @Column(name = "slot_id")
    private Integer slotId;

    @Column(name = "sku_id")
    private Integer skuId;

    // Bắt buộc phải override equals và hashCode theo chuẩn của Hibernate
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FlashSaleItemId that = (FlashSaleItemId) o;
        return Objects.equals(slotId, that.slotId) && Objects.equals(skuId, that.skuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId, skuId);
    }
}