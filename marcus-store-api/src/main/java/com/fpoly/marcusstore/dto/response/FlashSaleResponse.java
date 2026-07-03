package com.fpoly.marcusstore.dto.response;

import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashSaleResponse {
    private Integer slotId;
    private String name;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer quantityFlashSaleSlot;
    private Short status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
