package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderImeiAssignmentResponse {

    private Integer orderItemId;
    private String skuCode;
    private String productName;
    private Integer quantityOrdered;
    private Integer quantityAssigned;
    private List<ImeiDetailItem> availableImeis;
    private List<String> assignedImeis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImeiDetailItem {
        private Integer itemId;
        private String imeiCode;
        private Integer status;
        private String statusLabel;
        private LocalDateTime createdAt;
    }
}
