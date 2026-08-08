package com.fpoly.marcusstore.entity.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Marcus thêm: tách snapshot giao nhận và trạng thái kỹ thuật GHN khỏi bảng
 * Orders của luồng chung. Bảng dùng chung khóa order_id nên mỗi đơn chỉ có đúng
 * một cấu hình giao nhận, kể cả đơn nhận tại cửa hàng.
 */
@Entity
@Table(name = "Order_Shipping_Details")
@Getter
@Setter
public class OrderShippingDetail {

    @Id
    @Column(name = "order_id")
    private Integer orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "fulfillment_method", nullable = false, length = 30)
    private String fulfillmentMethod = "DELIVERY";

    @Column(name = "shipping_fee", precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(name = "shipping_subsidy", nullable = false, precision = 18, scale = 2)
    private BigDecimal shippingSubsidy = BigDecimal.ZERO;

    @Column(name = "customer_shipping_fee", precision = 18, scale = 2)
    private BigDecimal customerShippingFee;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "to_district_id")
    private Integer toDistrictId;

    @Column(name = "to_ward_code", length = 20)
    private String toWardCode;

    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    @Column(name = "ghn_integration_status", nullable = false, length = 30)
    private String ghnIntegrationStatus = "NOT_REQUIRED";

    @Column(name = "ghn_retry_count", nullable = false)
    private Integer ghnRetryCount = 0;

    @Column(name = "ghn_last_error", length = 500)
    private String ghnLastError;

    @Column(name = "ghn_last_attempt_at")
    private LocalDateTime ghnLastAttemptAt;
}
