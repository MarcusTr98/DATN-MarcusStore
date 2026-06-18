package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/revenue/daily")
    public ApiResponse<List<StatisticsResponseDTO>> getRevenueByDay() {
        return ApiResponse.success(statisticsService.getRevenueByDay());
    }

    @GetMapping("/revenue/monthly")
    public ApiResponse<List<StatisticsResponseDTO>> getRevenueByMonth() {
        return ApiResponse.success(statisticsService.getRevenueByMonth());
    }

    @GetMapping("/top-products")
    public ApiResponse<List<TopProductResponseDTO>> getTopSellingProducts(
            @RequestParam(defaultValue = "10") int topN) {
        return ApiResponse.success(statisticsService.getTopSellingProducts(topN));
    }

    @GetMapping("/orders/weekday")
    public ApiResponse<List<OrderByWeekdayResponseDTO>> getOrdersByWeekday() {
        return ApiResponse.success(statisticsService.getOrdersByWeekday());
    }

    @GetMapping("/revenue/by-brand")
    public ApiResponse<List<BrandRevenueResponseDTO>> getRevenueByBrand() {
        return ApiResponse.success(statisticsService.getRevenueByBrand());
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<LowStockResponseDTO>> getLowStockProducts() {
        return ApiResponse.success(statisticsService.getLowStockProducts());
    }

    @GetMapping("/top-customers")
    public ApiResponse<List<TopCustomerResponseDTO>> getTopCustomers(
            @RequestParam(defaultValue = "10") int topN) {
        return ApiResponse.success(statisticsService.getTopCustomers(topN));
    }

    @GetMapping("/recent-orders")
    public ApiResponse<List<RecentOrderResponseDTO>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statisticsService.getRecentOrders(limit));
    }
}