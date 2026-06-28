package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class OrderDetailResponse {
    private String orderCode;
    private String orderStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String trackingCode;
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String voucherCode;
    private String voucherDiscountType;
    private BigDecimal voucherDiscountValue;
    private BigDecimal voucherMaxDiscount;
    private List<OrderItemDetailResponse> items;
    private List<OrderStatusHistoryResponse> history;
}
