package com.fpoly.marcusstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportResponse(
        List<TransactionResponse> transactions,
        long totalCount,
        BigDecimal totalSuccessAmount,
        BigDecimal totalPendingAmount,
        BigDecimal totalFailedAmount,
        double successRate) {

}