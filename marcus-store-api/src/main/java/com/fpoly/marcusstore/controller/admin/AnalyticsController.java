package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsTrendPoint;
import com.fpoly.marcusstore.dto.analytics.ProductTrendResponse;
import com.fpoly.marcusstore.dto.analytics.CancellationReasonResponse;
import com.fpoly.marcusstore.dto.analytics.WarrantyAnalyticsResponse;
import com.fpoly.marcusstore.dto.analytics.BehaviorFunnelResponse;
import com.fpoly.marcusstore.service.analytics.BehaviorEventService;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.analytics.AnalyticsService;
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
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BehaviorEventService behaviorEventService;

    @GetMapping("/overview")
    public ApiResponse<AnalyticsOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(analyticsService.getOverview(fromDate, toDate));
    }

    @GetMapping("/sales-trend")
    public ApiResponse<List<AnalyticsTrendPoint>> getSalesTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(analyticsService.getSalesTrend(fromDate, toDate));
    }

    @GetMapping("/product-trends")
    public ApiResponse<List<ProductTrendResponse>> getProductTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analyticsService.getProductTrends(fromDate, toDate, limit));
    }

    @GetMapping("/cancellation-reasons")
    public ApiResponse<List<CancellationReasonResponse>> getCancellationReasons(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(analyticsService.getCancellationReasons(fromDate, toDate));
    }

    // Marcus thêm: API chỉ trả thống kê bảo hành tổng hợp, không lộ mô tả hoặc
    // khách hàng.
    @GetMapping("/warranty-quality")
    public ApiResponse<WarrantyAnalyticsResponse> getWarrantyQuality(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analyticsService.getWarrantyAnalytics(fromDate, toDate, limit));
    }

    @GetMapping("/behavior-funnel")
    public ApiResponse<BehaviorFunnelResponse> getBehaviorFunnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.success(behaviorEventService.funnel(fromDate, toDate));
    }
}
