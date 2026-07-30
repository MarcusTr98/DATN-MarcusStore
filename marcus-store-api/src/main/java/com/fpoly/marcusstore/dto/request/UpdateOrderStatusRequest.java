package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    // Marcus thêm: chỉ chấp nhận mã trạng thái chuẩn; luồng khách hủy có thể bỏ
    // trống status vì backend tự quyết định trạng thái CANCELLED.
    @Pattern(regexp = "^(PENDING|CONFIRMED|PROCESSING|READY_FOR_PICKUP|PACKED|SHIPPING|DELIVERED|COMPLETED|CANCELLED|FAILED)$", message = "Trạng thái đơn hàng không hợp lệ")
    private String status;

    @Size(max = 500, message = "Lý do hoặc ghi chú không được vượt quá 500 ký tự")
    private String note;

}
