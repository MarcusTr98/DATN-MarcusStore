package com.fpoly.marcusstore.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentOrderResponseDTO {
    private String orderCode;
    private String customerName;
    private String phone;
    private String paymentMethod;
    private String orderStatus;
    private BigDecimal totalAmount;

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime createdAt;
}