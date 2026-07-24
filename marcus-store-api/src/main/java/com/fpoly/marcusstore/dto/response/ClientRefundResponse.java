package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Marcus thêm DTO riêng cho client để không lộ mã phản hồi nội bộ của VNPAY.
@Data
@Builder
public class ClientRefundResponse {
    private BigDecimal amount;
    private BigDecimal shippingDeducted;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
