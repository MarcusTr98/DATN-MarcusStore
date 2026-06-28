package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoucherUsageResponse {
    private Integer voucherUsageId;
    private Integer voucherId;
    private String voucherCode;
    private String voucherDiscountType;
    private BigDecimal voucherDiscountValue;
    private BigDecimal voucherMaxDiscount;
    private Integer userId;
    private String userFullName;
    private String userEmail;
    private LocalDateTime usedAt;
}
