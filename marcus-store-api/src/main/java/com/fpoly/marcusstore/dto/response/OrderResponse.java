package com.fpoly.marcusstore.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private String orderCode;
    private String recipientName;
    private String recipiantPhone;
    private BigDecimal finalAmount;
    private String paymentMethod;

}
