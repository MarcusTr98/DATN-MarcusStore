package com.fpoly.marcusstore.entity.shopping;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_SCHEDULED = "SCHEDULED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Integer voucherId;

    @Column(name = "voucher_code", nullable = false, unique = true, length = 50)
    private String voucherCode;

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // 'PERCENT', 'AMOUNT', 'FREESHIP', 'GIFT'

    @Column(name = "max_discount_amount", precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Trạng thái: ACTIVE = đang sử dụng, INACTIVE = ngừng sử dụng, SCHEDULED = đã lên lịch (chưa đến ngày)
    @Column(name = "status", length = 20)
    private String status = STATUS_ACTIVE;

    // Đối tượng sử dụng: 'ALL' = tất cả, 'SPECIFIC' = khách cụ thể
    @Column(name = "target_type", length = 20)
    private String targetType = "ALL";
}
