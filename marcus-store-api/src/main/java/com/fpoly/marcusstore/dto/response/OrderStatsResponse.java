package com.fpoly.marcusstore.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {
    // tổng số đơn hàng trong hệ thống
    private Long total;
    // số đơn đang chờ xác nhận
    private Long pending;
    // số đơn đã các nhận
    private Long confirmed;
    // số đơn đang giao;
    private Long shipping;
    // số đơn hoàn thành
    private Long completed;
    // số đơn đã hủy
    private Long cancelled;
}
