package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFlashSaleStatusRequest {
// dùng cho admin hủy chiến dịch từ đã leen lịch sang đã hủy
    @NotNull(message = "Status không được để trống")
    private Short status;
}
