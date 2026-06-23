package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AddVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String voucherCode;
    @NotNull(message = "Giá trị giảm không được để trống")
    private BigDecimal discountValue;
    @NotBlank(message = "Hãy chọn kiểu giảm giá hợp lệ")
    private String discountType; // 'PERCENT', 'AMOUNT', 'FREESHIP', 'GIFT'
    private BigDecimal maxDiscountAmount;
    @NotNull(message = "Đơn tối thiểu không đuợc bỏ trống")
    private BigDecimal minOrderValue;
    @NotNull(message = "Ngày bắt đầu không đuợc bỏ trống")
    private LocalDateTime startDate;
    @NotNull(message = "Ngày kết thúc không đuợc bỏ trống")
    private LocalDateTime endDate;
    private Boolean isActive;
    private String description;

    // FREESHIP: khu vực áp dụng (NORTH, CENTRAL, SOUTH, null = ALL)
    private String region;

    // Đối tượng sử dụng
    @NotBlank(message = "Hãy chọn đối tượng sử dụng")
    private String targetType; // 'ALL' = tất cả, 'SPECIFIC' = khách cụ thể
    private List<Integer> targetUserIds; // Danh sách user IDs khi targetType = 'SPECIFIC'
}
