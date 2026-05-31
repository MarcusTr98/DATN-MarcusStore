package com.fpoly.marcusstore.entity.promotion;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class FlashSaleItemId implements Serializable {

    @Column(name = "slot_id")
    private Integer slotId;

    @Column(name = "sku_id")
    private Integer skuId;
}