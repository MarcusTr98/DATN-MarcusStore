package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private String orderCode;
    private BigDecimal amount;
    private String type;
    private String status;
    private String note;
    private LocalDateTime createdAt;
}