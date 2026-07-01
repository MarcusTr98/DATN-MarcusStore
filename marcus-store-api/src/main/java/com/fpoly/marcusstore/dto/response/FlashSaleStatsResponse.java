package com.fpoly.marcusstore.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleStatsResponse {
    private long totalSlots;             // tổng số flash sale theo filter hiện tại
    private long activeSlots;            // số slot đang chạy (status=2, trong khung giờ)
    private long upcomingSlots;          // số slot sắp diễn ra (status=1, startDate > now)
    private long totalActiveProducts;    // TỔNG QUANTITY của tất cả SKU trong các slot đang chạy

}
