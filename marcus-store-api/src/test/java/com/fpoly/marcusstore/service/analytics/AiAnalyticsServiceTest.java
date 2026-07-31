package com.fpoly.marcusstore.service.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpoly.marcusstore.repository.analytics.AiAnalyticsReportRepository;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

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
                new ObjectMapper(),
                reportRepository,
                mock(AiUsageEventService.class));
        ReflectionTestUtils.setField(aiAnalyticsService, "apiKey", "");
        ReflectionTestUtils.setField(aiAnalyticsService, "model", "gemini-3.5-flash-lite");
        ReflectionTestUtils.setField(
                aiAnalyticsService,
                "baseUrl",
                "https://generativelanguage.googleapis.com");
    }

    @Test
    void missingApiKeyStopsBeforeLoadingBusinessData() {
        assertThrows(
                IllegalStateException.class,
                () -> aiAnalyticsService.generateReport(null, null));

        // Marcus kiểm tra: cấu hình thiếu không được chạy query thừa hoặc gửi dữ liệu.
        verifyNoInteractions(analyticsService);
    }
}
