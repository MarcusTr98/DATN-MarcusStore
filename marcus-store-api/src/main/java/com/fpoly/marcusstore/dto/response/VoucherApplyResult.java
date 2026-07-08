package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherApplyResult {
    private Integer voucherId;
    private String voucherCode;
    private String discountType;
    private BigDecimal discountAmount;
    private BigDecimal freeshipAmount;
    private boolean applied;
    private String message;
    /**
     * Mã lỗi dạng chuỗi (VD: VOUCHER_INACTIVE, VOUCHER_EXPIRED, VOUCHER_QUOTA_EXHAUSTED).
     * Chỉ có giá trị khi applied=false. Cho phép FE phân biệt loại lỗi
     * để hiển thị thông báo phù hợp và xử lý nghiệp vụ (mở lại modal chọn voucher).
     */
    private String errorCode;
}
