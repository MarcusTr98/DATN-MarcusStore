package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.repository.analytics.AiUsageEventRepository;
import com.fpoly.marcusstore.repository.analytics.AiUsageEventRepository.AiUsageSummaryRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiUsageEventServiceTest {

    @Mock
    private AiUsageEventRepository repository;

    @Test
    void ignoresInvalidSessionInsteadOfStoringPersonalFallback() {
        AiUsageEventService service = new AiUsageEventService(repository);

        service.recordChatResult("not-a-uuid", null, true, 250);

        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }

    @Test
    void storesOnlyAnonymousTelemetryAndClampsResponseTime() {
        AiUsageEventService service = new AiUsageEventService(repository);
        String sessionId = "123e4567-e89b-42d3-a456-426614174000";

        String adviceId = "123e4567-e89b-42d3-a456-426614174001";
        service.recordChatResult(sessionId, adviceId, true, 999_999);

        verify(repository).insert(eq(sessionId), eq(adviceId), eq("CHAT_RESPONSE"), eq(null), eq(120_000));
    }

    @Test
    void calculatesRatesFromAggregateOnly() {
        AiUsageSummaryRow row = new AiUsageSummaryRow(8, 2, 3, 10, 10, 1_250);
        when(repository.summarize(any(), any())).thenReturn(row);

        var response = new AiUsageEventService(repository)
                .summarize(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30));

        assertThat(response.successRate()).isEqualTo(80.0);
        assertThat(response.clickThroughRate()).isEqualTo(37.5);
        assertThat(response.averageResponseTimeMs()).isEqualTo(1_250);
    }

    @Test
    void rejectsInvertedOrExcessiveRange() {
        AiUsageEventService service = new AiUsageEventService(repository);

        assertThatThrownBy(() -> service.summarize(
                LocalDate.of(2026, 7, 30),
                LocalDate.of(2026, 7, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void feedbackMustBelongToTheSameAdviceAndSession() {
        String sessionId = "123e4567-e89b-42d3-a456-426614174000";
        String adviceId = "123e4567-e89b-42d3-a456-426614174001";
        when(repository.existsChatResponse(sessionId, adviceId)).thenReturn(false);

        assertThatThrownBy(() -> new AiUsageEventService(repository)
                .recordFeedback(sessionId, adviceId, true))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).insert(any(), any(), any(), any(), any());
    }
}
