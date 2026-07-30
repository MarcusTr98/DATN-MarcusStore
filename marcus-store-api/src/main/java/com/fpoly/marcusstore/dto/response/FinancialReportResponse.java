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
        // Marcus thêm: hai số này giải thích chênh lệch giữa doanh thu và
        // dòng tiền, không đánh đồng tiền đơn hủy đang chờ hoàn với doanh thu.
        BigDecimal recognizedRevenue,
        BigDecimal unsettledCancellationAmount,
        BigDecimal totalPendingAmount,
        BigDecimal totalFailedAmount,
        double successRate) {

}
