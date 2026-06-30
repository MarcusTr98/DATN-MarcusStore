package com.fpoly.marcusstore.entity.shopping;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "Order_Transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 50)
    private String type; // VNPAY_PAYMENT, COD_COLLECTION, REFUND

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, PENDING, FAILED

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @JsonProperty("orderCode")
    public String getOrderCode() {
        return this.order != null ? this.order.getOrderCode() : "N/A";
    }

}