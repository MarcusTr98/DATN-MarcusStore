package com.fpoly.marcusstore.entity.shopping;

import com.fpoly.marcusstore.entity.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Refund_Requests", uniqueConstraints = {
        @UniqueConstraint(name = "UX_RefundRequests_IdempotencyKey", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "UX_RefundRequests_RequestCode", columnNames = "request_code")
})
@Getter
@Setter
// Marcus thêm entity lưu vòng đời refund độc lập với trạng thái đơn hàng.
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_transaction_id", nullable = false)
    private OrderTransaction paymentTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_transaction_id")
    private OrderTransaction refundTransaction;

    @Column(name = "request_code", nullable = false, length = 32)
    private String requestCode;

    @Column(name = "idempotency_key", nullable = false, length = 150)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "shipping_deducted", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingDeducted = BigDecimal.ZERO;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "provider_response_id", length = 100)
    private String providerResponseId;

    @Column(name = "provider_refund_transaction_id", length = 100)
    private String providerRefundTransactionId;

    @Column(name = "provider_response_code", length = 20)
    private String providerResponseCode;

    @Column(name = "provider_transaction_status", length = 20)
    private String providerTransactionStatus;

    @Column(name = "provider_message", length = 500)
    private String providerMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    // Marcus thêm dữ liệu đối soát riêng, không dùng chung bộ đếm gửi refund.
    @Column(name = "reconciliation_attempts", nullable = false)
    private Integer reconciliationAttempts = 0;

    @Column(name = "last_reconciled_at")
    private LocalDateTime lastReconciledAt;

    @Column(name = "next_reconciliation_at")
    private LocalDateTime nextReconciliationAt;

    @Column(name = "last_reconciliation_message", length = 500)
    private String lastReconciliationMessage;

    // Marcus thêm audit xác nhận thủ công, chỉ được dùng trên Sandbox.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manually_confirmed_by")
    private User manuallyConfirmedBy;

    @Column(name = "manually_confirmed_at")
    private LocalDateTime manuallyConfirmedAt;

    @Column(name = "manual_confirmation_note", length = 500)
    private String manualConfirmationNote;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion = 0L;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
