package com.fpoly.marcusstore.dto.response;

import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusHistoryResponse {
    private String status;
    private String title;
    private String note;
    private LocalDateTime createdAt;
    private String createdByName;
}
