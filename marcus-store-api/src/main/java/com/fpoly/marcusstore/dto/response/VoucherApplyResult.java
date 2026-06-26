package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherApplyResult {
    private Integer voucherId;
    private String voucherCode;
    private String discountType;
    private BigDecimal discountAmount;
    private BigDecimal freeshipAmount;
    private boolean applied;
    private String message;
}
