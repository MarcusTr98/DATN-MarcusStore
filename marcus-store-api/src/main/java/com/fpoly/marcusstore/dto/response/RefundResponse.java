package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponse {
    private Long refundId;
    private String orderCode;
    private Integer paymentTransactionId;
    private Integer refundTransactionId;
    private String requestCode;
    private BigDecimal amount;
    private BigDecimal shippingDeducted;
    private String reason;
    private String status;
    private String requestedBy;
    private String approvedBy;
    private Integer retryCount;
    private Integer maxRetries;
    private String providerRefundTransactionId;
    private String providerResponseCode;
    private String providerTransactionStatus;
    private String providerMessage;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private Integer reconciliationAttempts;
    private LocalDateTime lastReconciledAt;
    private LocalDateTime nextReconciliationAt;
    private String lastReconciliationMessage;
    private String manuallyConfirmedBy;
    private LocalDateTime manuallyConfirmedAt;
    private String manualConfirmationNote;
    // Marcus thêm cờ để UI chỉ hiện xác nhận thủ công khi backend đang chạy
    // Sandbox.
    private Boolean manualConfirmationAllowed;
}
