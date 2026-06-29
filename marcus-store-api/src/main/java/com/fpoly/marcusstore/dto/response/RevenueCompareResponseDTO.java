package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueCompareResponseDTO {
    private List<PeriodData> current;
    private List<PeriodData> previous;
    private String currentLabel;
    private String previousLabel;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodData {
        private String label;
        private String sublabel;
        private double revenue;
    }
}