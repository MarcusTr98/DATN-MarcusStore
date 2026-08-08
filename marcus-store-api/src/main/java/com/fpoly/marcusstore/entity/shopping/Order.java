package com.fpoly.marcusstore.entity.shopping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Orders")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    // Marcus thêm: khóa idempotency do Checkout client sinh. Một request được
    // gửi lại do double click, F5 hoặc retry mạng chỉ được ánh xạ tới một đơn.
    // Marcus sửa: SQL Server dùng filtered unique index để vẫn cho phép nhiều đơn
    // cũ có NULL.
    @Column(name = "checkout_request_id", length = 64)
    private String checkoutRequestId;

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 15)
    private String recipientPhone;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "payment_status", length = 50)
    private String paymentStatus = "UNPAID";

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "order_status", length = 50)
    private String orderStatus = "PENDING";

    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    @JsonIgnore
    private Voucher voucher;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> orderItems = new ArrayList<>();

    // Marcus thêm: các trường giao nhận/GHN và hủy đơn được tách khỏi Orders
    // nhưng giữ method miền nghiệp vụ trên Order để không làm rò cấu trúc lưu trữ
    // sang Checkout, email và response mapper.
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private OrderShippingDetail shippingDetail;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private OrderCancellation cancellation;

    private OrderShippingDetail ensureShippingDetail() {
        if (shippingDetail == null) {
            shippingDetail = new OrderShippingDetail();
            shippingDetail.setOrder(this);
        }
        return shippingDetail;
    }

    private OrderCancellation ensureCancellation() {
        if (cancellation == null) {
            cancellation = new OrderCancellation();
            cancellation.setOrder(this);
        }
        return cancellation;
    }

    public String getFulfillmentMethod() {
        return shippingDetail == null ? "DELIVERY" : shippingDetail.getFulfillmentMethod();
    }

    public void setFulfillmentMethod(String value) {
        ensureShippingDetail().setFulfillmentMethod(value);
    }

    public BigDecimal getShippingFee() {
        return shippingDetail == null ? null : shippingDetail.getShippingFee();
    }

    public void setShippingFee(BigDecimal value) {
        ensureShippingDetail().setShippingFee(value);
    }

    public BigDecimal getShippingSubsidy() {
        return shippingDetail == null ? BigDecimal.ZERO : shippingDetail.getShippingSubsidy();
    }

    public void setShippingSubsidy(BigDecimal value) {
        ensureShippingDetail().setShippingSubsidy(value);
    }

    public BigDecimal getCustomerShippingFee() {
        return shippingDetail == null ? null : shippingDetail.getCustomerShippingFee();
    }

    public void setCustomerShippingFee(BigDecimal value) {
        ensureShippingDetail().setCustomerShippingFee(value);
    }

    public String getTrackingCode() {
        return shippingDetail == null ? null : shippingDetail.getTrackingCode();
    }

    public void setTrackingCode(String value) {
        ensureShippingDetail().setTrackingCode(value);
    }

    public Integer getToDistrictId() {
        return shippingDetail == null ? null : shippingDetail.getToDistrictId();
    }

    public void setToDistrictId(Integer value) {
        ensureShippingDetail().setToDistrictId(value);
    }

    public String getToWardCode() {
        return shippingDetail == null ? null : shippingDetail.getToWardCode();
    }

    public void setToWardCode(String value) {
        ensureShippingDetail().setToWardCode(value);
    }

    public String getDeliveryNote() {
        return shippingDetail == null ? null : shippingDetail.getDeliveryNote();
    }

    public void setDeliveryNote(String value) {
        ensureShippingDetail().setDeliveryNote(value);
    }

    public String getGhnIntegrationStatus() {
        return shippingDetail == null ? "NOT_REQUIRED" : shippingDetail.getGhnIntegrationStatus();
    }

    public void setGhnIntegrationStatus(String value) {
        ensureShippingDetail().setGhnIntegrationStatus(value);
    }

    public Integer getGhnRetryCount() {
        return shippingDetail == null ? 0 : shippingDetail.getGhnRetryCount();
    }

    public void setGhnRetryCount(Integer value) {
        ensureShippingDetail().setGhnRetryCount(value);
    }

    public String getGhnLastError() {
        return shippingDetail == null ? null : shippingDetail.getGhnLastError();
    }

    public void setGhnLastError(String value) {
        ensureShippingDetail().setGhnLastError(value);
    }

    public LocalDateTime getGhnLastAttemptAt() {
        return shippingDetail == null ? null : shippingDetail.getGhnLastAttemptAt();
    }

    public void setGhnLastAttemptAt(LocalDateTime value) {
        ensureShippingDetail().setGhnLastAttemptAt(value);
    }

    public String getCancellationReasonCode() {
        return cancellation == null ? null : cancellation.getReasonCode();
    }

    public void setCancellationReasonCode(String value) {
        ensureCancellation().setReasonCode(value);
    }

    public String getCancellationActor() {
        return cancellation == null ? null : cancellation.getActorType();
    }

    public void setCancellationActor(String value) {
        ensureCancellation().setActorType(value);
    }

    public LocalDateTime getCancelledAt() {
        return cancellation == null ? null : cancellation.getCancelledAt();
    }

    public void setCancelledAt(LocalDateTime value) {
        ensureCancellation().setCancelledAt(value);
    }

    public String getCancellationDetail() {
        return cancellation == null ? null : cancellation.getDetail();
    }

    public void setCancellationDetail(String value) {
        ensureCancellation().setDetail(value);
    }
}
