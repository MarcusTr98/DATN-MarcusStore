package com.fpoly.marcusstore.dto.response;

public record VoucherStatsResponse(
        long total,
        long active,
        long percent,
        long amount
) {
}
