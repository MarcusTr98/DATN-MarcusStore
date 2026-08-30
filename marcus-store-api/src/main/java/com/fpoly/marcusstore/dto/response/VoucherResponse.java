package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherResponse {
    private Integer voucherId;
    private String voucherCode;
    private BigDecimal discountValue;
    private String discountType;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantity;
    private String status; // 'ACTIVE', 'INACTIVE', 'SCHEDULED'

    // Đối tượng sử dụng: 'ALL' = tất cả, 'SPECIFIC' = khách cụ thể
    private String targetType;
    private List<Integer> targetUserIds;
    private Integer targetUserCount;

    // Thông tin UserVoucher (khi trả về voucher của user)
    private Integer userVoucherId;
    private Boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime assignedAt;
}
