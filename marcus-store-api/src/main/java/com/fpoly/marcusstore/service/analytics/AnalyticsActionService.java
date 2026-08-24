package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.dto.analytics.*;
import com.fpoly.marcusstore.entity.analytics.AnalyticsAction;
import com.fpoly.marcusstore.repository.analytics.AnalyticsActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsActionService {
    private static final List<String> OPEN_STATUSES = List.of("ACCEPTED", "IN_PROGRESS");
    private final AnalyticsActionRepository repository;

    @Transactional(readOnly = true)
    public List<AnalyticsActionResponse> list() {
        return repository.findTop50ByOrderByUpdatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AnalyticsActionResponse accept(AnalyticsActionRequest request, String username) {
        String title = request.title().trim();
        if (repository.existsByTitleAndStatusIn(title, OPEN_STATUSES))
            throw new IllegalArgumentException("Hành động này đang được theo dõi.");
        AnalyticsAction action = new AnalyticsAction();
        action.setTitle(title);
        action.setReason(request.reason().trim());
        action.setPriority(request.priority());
        action.setStatus("ACCEPTED");
        action.setOwnerUsername(username);
        return toResponse(repository.save(action));
    }

    @Transactional
    public AnalyticsActionResponse updateStatus(Long id, AnalyticsActionStatusRequest request) {
        AnalyticsAction action = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hành động Analytics."));
        action.setStatus(request.status());
        return toResponse(repository.save(action));
    }

    private AnalyticsActionResponse toResponse(AnalyticsAction action) {
        return new AnalyticsActionResponse(action.getActionId(), action.getTitle(), action.getReason(),
                action.getPriority(), action.getStatus(), action.getOwnerUsername(),
                action.getCreatedAt(), action.getUpdatedAt());
    }
}
