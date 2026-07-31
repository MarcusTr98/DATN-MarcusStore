package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemResponse {
    private Integer itemId;
    private Integer skuId;
    private String skuCode;
    private String imeiCode;
    private Integer status;
    private String statusLabel;
    private Integer orderItemId;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
