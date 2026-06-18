package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderByWeekdayResponseDTO {
    private String dayLabel; // "Thứ 2", "Thứ 3"...
    private Long totalOrders;
}