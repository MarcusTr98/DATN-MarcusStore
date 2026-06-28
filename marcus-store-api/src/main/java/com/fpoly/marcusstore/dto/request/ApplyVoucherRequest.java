package com.fpoly.marcusstore.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class ApplyVoucherRequest {
    private String voucherCode;
    private BigDecimal orderAmount;
    private BigDecimal shippingFee;
}
