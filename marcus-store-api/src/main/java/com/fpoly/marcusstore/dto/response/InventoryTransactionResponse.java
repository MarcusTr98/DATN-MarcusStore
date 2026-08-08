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
public class InventoryTransactionResponse {
    private Integer transactionId;
    private Integer skuId;
    private String skuCode;
    private String productName;
    private String transactionType;
    private Integer quantityBefore;
    private Integer quantityChanged;
    private Integer quantityAfter;
    private String referenceType;
    private Integer referenceId;
    private String note;
    private String createdByName;
    private LocalDateTime createdAt;
}