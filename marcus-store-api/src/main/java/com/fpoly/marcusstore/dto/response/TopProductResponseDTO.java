package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopProductResponseDTO {

    private String productName;
    private Long totalSold;
    private BigDecimal revenue;
}