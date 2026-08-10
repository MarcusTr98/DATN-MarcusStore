package com.fpoly.marcusstore.dto.analytics;

public record BehaviorFunnelResponse(
        long productViewSessions,
        long checkoutSessions,
        long orderSessions,
        long paidSessions,
        long aiQuestionSessions,
        long aiProductClickSessions,
        double viewToCheckoutRate,
        double checkoutToOrderRate,
        double orderToPaymentRate,
        double aiClickRate) {
}
