package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.dto.analytics.AnalyticsActionRequest;
import com.fpoly.marcusstore.dto.analytics.AnalyticsActionStatusRequest;
import com.fpoly.marcusstore.entity.analytics.AnalyticsAction;
import com.fpoly.marcusstore.repository.analytics.AnalyticsActionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnalyticsActionServiceTest {
    private AnalyticsActionRepository repository;
    private AnalyticsActionService service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyticsActionRepository.class);
        service = new AnalyticsActionService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void acceptsNewActionWithAuthenticatedOwner() {
        var response = service.accept(new AnalyticsActionRequest("Kiểm tra tồn kho", "Sản lượng tăng", "HIGH"),
                "admin");
        assertEquals("ACCEPTED", response.status());
        assertEquals("admin", response.ownerUsername());
        verify(repository).save(any(AnalyticsAction.class));
    }

    @Test
    void preventsDuplicateOpenAction() {
        when(repository.existsByTitleAndStatusIn("Kiểm tra tồn kho", List.of("ACCEPTED", "IN_PROGRESS")))
                .thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.accept(
                new AnalyticsActionRequest("Kiểm tra tồn kho", "Lý do", "HIGH"), "admin"));
    }

    @Test
    void updatesWorkflowStatus() {
        AnalyticsAction action = new AnalyticsAction();
        action.setStatus("ACCEPTED");
        action.setTitle("A");
        action.setReason("B");
        action.setPriority("LOW");
        action.setOwnerUsername("admin");
        when(repository.findById(1L)).thenReturn(Optional.of(action));
        var response = service.updateStatus(1L, new AnalyticsActionStatusRequest("DONE"));
        assertEquals("DONE", response.status());
    }
}
