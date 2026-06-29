package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderByWeekdayResponseDTO {
    private String dayLabel;
    private Long totalOrders;
}