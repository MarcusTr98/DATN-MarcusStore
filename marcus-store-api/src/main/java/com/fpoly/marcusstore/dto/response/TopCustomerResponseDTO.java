package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TopCustomerResponseDTO {
    private String customerName;
    private String email;
    private Long totalOrders;
    private BigDecimal totalSpent;
    private Double contributionPercent;
}