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
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
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

    @GetMapping("/kpi-compare")
    public ApiResponse<KpiCompareDTO> getKpiCompare(
            @RequestParam(required = false, defaultValue = "today") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(statisticsService.getKpiCompare(period, startDate, endDate));
    }

    // ── FIX 7: phân trang backend — top-products ──────────────────────────────
    @GetMapping("/top-products")
    public ApiResponse<PagedResponseDTO<TopProductResponseDTO>> getTopSellingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(
                statisticsService.getTopSellingProducts(dates[0], dates[1], keyword, page, size));
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

    // ── FIX 7: phân trang backend — low-stock ────────────────────────────────
    @GetMapping("/low-stock")
    public ApiResponse<PagedResponseDTO<LowStockResponseDTO>> getLowStockProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(
                statisticsService.getLowStockProducts(keyword, brand, status, page, size));
    }

    // ── FIX 7: phân trang backend — top-customers ─────────────────────────────
    @GetMapping("/top-customers")
    public ApiResponse<PagedResponseDTO<TopCustomerResponseDTO>> getTopCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(
                statisticsService.getTopCustomers(dates[0], dates[1], keyword, page, size));
    }

    // ── FIX 7: phân trang backend — recent-orders ─────────────────────────────
    @GetMapping("/recent-orders")
    public ApiResponse<PagedResponseDTO<RecentOrderResponseDTO>> getRecentOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(
                statisticsService.getRecentOrders(dates[0], dates[1], keyword, status, brand, page, size));
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

    // ── FIX 7: phân trang backend — pending-orders ────────────────────────────
    @GetMapping("/pending-orders")
    public ApiResponse<PagedResponseDTO<RecentOrderResponseDTO>> getPendingOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(statisticsService.getPendingOrders(keyword, page, size));
    }

    @GetMapping("/payment-stats")
    public ApiResponse<PaymentStatsDTO> getPaymentStats(
            @RequestParam(required = false, defaultValue = "today") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate[] dates = StatisticsService.resolveDateRange(startDate, endDate, period);
        return ApiResponse.success(statisticsService.getPaymentStats(dates[0], dates[1]));
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