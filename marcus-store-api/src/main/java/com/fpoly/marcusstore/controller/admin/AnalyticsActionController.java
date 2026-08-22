package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.analytics.*;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.analytics.AnalyticsActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics/actions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsActionController {
    private final AnalyticsActionService service;

    @GetMapping
    public ApiResponse<List<AnalyticsActionResponse>> list() {
        return ApiResponse.success(service.list());
    }

    @PostMapping
    public ApiResponse<AnalyticsActionResponse> accept(
            @Valid @RequestBody AnalyticsActionRequest request, Authentication authentication) {
        return ApiResponse.success(service.accept(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<AnalyticsActionResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody AnalyticsActionStatusRequest request) {
        return ApiResponse.success(service.updateStatus(id, request));
    }
}
