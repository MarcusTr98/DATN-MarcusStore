package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Integer voucherId;
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
