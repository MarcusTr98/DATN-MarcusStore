package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/revenue/daily")
    public ApiResponse<List<StatisticsResponseDTO>> getRevenueByDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getRevenueByDay(dates[0], dates[1]));
    }

    @GetMapping("/kpi-summary")
    public ApiResponse<KpiSummaryDTO> getKpiSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getKpiSummary(dates[0], dates[1]));
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponseDTO>> getTopSellingProducts(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getTopSellingProducts(topN, dates[0], dates[1], keyword));
    }

    @GetMapping("/orders/weekday")
    public ApiResponse<List<OrderByWeekdayResponseDTO>> getOrdersByWeekday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getOrdersByWeekday(dates[0], dates[1]));
    }

    @GetMapping("/revenue/by-brand")
    public ApiResponse<List<BrandRevenueResponseDTO>> getRevenueByBrand(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getRevenueByBrand(dates[0], dates[1]));
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<LowStockResponseDTO>> getLowStockProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(statisticsService.getLowStockProducts(keyword, brand, status));
    }

    @GetMapping("/top-customers")
    public ApiResponse<List<TopCustomerResponseDTO>> getTopCustomers(
            @RequestParam(defaultValue = "10") int topN,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getTopCustomers(topN, dates[0], dates[1], keyword));
    }

    @GetMapping("/recent-orders")
    public ApiResponse<List<RecentOrderResponseDTO>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getRecentOrders(limit, dates[0], dates[1], keyword, status, brand));
    }

    @GetMapping("/revenue/compare")
public ApiResponse<RevenueCompareResponseDTO> getRevenueCompare(
        @RequestParam(required = false, defaultValue = "month") String period,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
    return ApiResponse.success(statisticsService.getRevenueCompare(period, startDate, endDate));
}

    @GetMapping("/pending-orders/count")
    public ApiResponse<Long> countPendingOrders() {
        return ApiResponse.success(statisticsService.countPendingOrders());
    }

    @GetMapping("/users/new")
    public ApiResponse<List<NewUserStatsDTO>> getNewUsers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getNewUsers(period, dates[0], dates[1]));
    }
}