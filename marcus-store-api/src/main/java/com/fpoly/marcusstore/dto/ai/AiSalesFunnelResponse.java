package com.fpoly.marcusstore.dto.ai;

// Marcus thêm: funnel riêng chứng minh AI hỗ trợ từ tư vấn đến thanh toán.
public record AiSalesFunnelResponse(
        long questionSessions,
        long responseSessions,
        long helpfulSessions,
        long clickSessions,
        long checkoutSessions,
        long orderSessions,
        long paidSessions,
        double responseRate,
        double helpfulRate,
        double clickRate,
        double checkoutRate,
        double orderRate,
        double paidRate) {
}
