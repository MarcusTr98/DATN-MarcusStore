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
    private String cancellationReasonCode;
    private String cancellationReasonLabel;
    private String cancellationActor;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    // Marcus thêm: client/admin chọn đúng giao diện giao tận nơi hoặc nhận tại
    // quầy.
    private String fulfillmentMethod;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
    private LocalDateTime paymentDate;
    // Marcus thêm: thời hạn VNPAY được backend tính thống nhất cho Client/Admin.
    private LocalDateTime paymentExpiresAt;
    private String trackingCode;
    // Marcus thêm: Admin nhìn được tình trạng kết nối GHN và quyết định retry.
    private String ghnIntegrationStatus;
    private Integer ghnRetryCount;
    private String ghnLastError;
    private LocalDateTime ghnLastAttemptAt;
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
    // marcus thêm
    private BigDecimal shippingSubsidy;
    private String deliveryNote;
    private OrderAssignmentResponse assignment;
}
