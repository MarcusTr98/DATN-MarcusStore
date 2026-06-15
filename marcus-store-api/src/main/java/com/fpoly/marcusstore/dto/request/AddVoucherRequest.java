package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class AddVoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    private String voucherCode;
    @NotNull(message = "Giá trị giảm không được để trống")
    @Min(value = 1, message = "Giá trị giảm không được nhỏ hơn 0")
    private BigDecimal discountValue;
    @NotBlank(message = "Hãy chọn kiểu giảm giá hợp lệ")
    private String discountType;
    private BigDecimal maxDiscountAmount;
    @NotNull(message = "Đơn tối thiểu không đuợc bỏ trống")
    @Min(value = 0, message = "Đơn tối thiểu không đựợc nhỏ hơn 0")
    private BigDecimal minOrderValue;
    @NotNull(message = "Ngày bắt đầu không đuợc bỏ trống")
    private LocalDateTime startDate;
    @NotNull(message = "Ngày kết thúc không đuợc bỏ trống")
    private LocalDateTime endDate;
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được nhỏ hơn hơn 0")
    private Integer quantity;
    private Boolean isActive;
}
