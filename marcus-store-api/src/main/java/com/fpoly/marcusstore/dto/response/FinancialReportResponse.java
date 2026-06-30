package com.fpoly.marcusstore.dto.response;

import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import java.math.BigDecimal;
import java.util.List;

public record FinancialReportResponse(
        List<OrderTransaction> transactions,
        long totalCount,
        BigDecimal totalSuccessAmount,
        BigDecimal totalPendingAmount,
        BigDecimal totalFailedAmount,
        double successRate) {
}