package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleResponse {

    private Integer slotId;
    private String name;
    private String bannerImageUrl;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantityFlashSaleSlot;
    private Short status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<FlashSaleItemResponse> items;
}
