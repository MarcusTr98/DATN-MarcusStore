package com.fpoly.marcusstore.service.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.repository.analytics.AiAnalyticsReportRepository;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fpoly.marcusstore.dto.analytics.AnalyticsMetric;
import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.dto.analytics.AnalyticsRateMetric;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AiAnalyticsServiceTest {

    private AnalyticsService analyticsService;
    private AiAnalyticsService aiAnalyticsService;
    private AiAnalyticsReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        analyticsService = mock(AnalyticsService.class);
        reportRepository = mock(AiAnalyticsReportRepository.class);
        aiAnalyticsService = new AiAnalyticsService(
                analyticsService,
                new ObjectMapper().findAndRegisterModules(),
                reportRepository,
                mock(AiUsageEventService.class));
    }

    @Test
    void missingApiKeyStillReturnsAlgorithmReport() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 30);
        AnalyticsMetric sales = new AnalyticsMetric(
                BigDecimal.valueOf(120_000_000), BigDecimal.valueOf(100_000_000), 20D);
        AnalyticsMetric zero = new AnalyticsMetric(BigDecimal.ZERO, BigDecimal.ZERO, 0D);
        when(analyticsService.getOverview(any(), any())).thenReturn(new AnalyticsOverviewResponse(
                new AnalyticsPeriod(from, to, from.minusDays(30), from.minusDays(1), 30),
                sales, zero, zero, zero,
                new AnalyticsRateMetric(80, 75, 5), new AnalyticsRateMetric(10, 12, -2), zero, zero));
        when(analyticsService.getProductTrends(any(), any(), any(Integer.class))).thenReturn(List.of());
        when(analyticsService.getSalesTrend(any(), any())).thenReturn(List.of());
        when(analyticsService.getCancellationReasons(any(), any())).thenReturn(List.of());

        var response = aiAnalyticsService.generateReport(from, to);

        // Marcus kiểm tra: thiếu key/quota chỉ hạ cấp sang thuật toán, không làm chết tab.
        assertThat(response.source()).isEqualTo("ALGORITHM");
        assertThat(response.signals().getFirst().evidenceId()).isEqualTo("ALG-SALES-CHANGE");
    }
}
