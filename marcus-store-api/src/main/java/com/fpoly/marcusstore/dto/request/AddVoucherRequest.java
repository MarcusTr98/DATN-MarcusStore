package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AddVoucherRequest {
    private String voucherCode;
    private BigDecimal discountValue;
    private String discountType;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantity;
    private Boolean isActive;
}
