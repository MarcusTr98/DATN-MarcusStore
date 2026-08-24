package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.ai.AiUsageSummaryResponse;
import com.fpoly.marcusstore.dto.ai.AiSalesFunnelResponse;
import com.fpoly.marcusstore.repository.analytics.BehaviorEventRepository.TopAiProductClick;
import com.fpoly.marcusstore.service.analytics.BehaviorEventService;
import com.fpoly.marcusstore.service.ai.AiUsageEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-advisor")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiAdvisorController {

    private final BehaviorEventService behaviorEventService;
    private final AiUsageEventService usageEventService;

    @GetMapping("/top-clicked-products")
    public ApiResponse<List<TopAiProductClick>> topClickedProducts() {
        return ApiResponse.success(behaviorEventService.topAiProductClicks());
    }

    @GetMapping("/usage-summary")
    public ApiResponse<AiUsageSummaryResponse> usageSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(usageEventService.summarize(fromDate, toDate));
    }

    @GetMapping("/sales-funnel")
    public ApiResponse<AiSalesFunnelResponse> salesFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(usageEventService.salesFunnel(fromDate, toDate));
    }
}
