package com.fpoly.marcusstore.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportResponse(
                List<TransactionResponse> transactions,
                long totalCount,
                // Marcus thêm: tách tiền vào, tiền hoàn và dòng tiền ròng để không cộng
                // giao dịch REFUND như doanh thu.
                BigDecimal totalSuccessAmount,
                BigDecimal totalRefundAmount,
                BigDecimal netCashFlow,
                BigDecimal totalPendingAmount,
                BigDecimal totalFailedAmount,
                double successRate) {

}
