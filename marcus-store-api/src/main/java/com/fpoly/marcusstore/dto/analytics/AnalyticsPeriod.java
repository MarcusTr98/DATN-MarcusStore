package com.fpoly.marcusstore.dto.analytics;

import java.time.LocalDate;

public record AnalyticsPeriod(
                LocalDate fromDate,
                LocalDate toDate,
                LocalDate previousFromDate,
                LocalDate previousToDate,
                long numberOfDays) {
}
