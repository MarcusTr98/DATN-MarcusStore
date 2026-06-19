package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.repository.statistics.StatisticsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private StatisticsRepository statisticsRepository;

    private static final String[] WEEKDAY_LABELS = {
            "", "Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"
    };

    private static final int LOW_STOCK_THRESHOLD = 5;
    @Transactional(readOnly = true)
    public List<StatisticsResponseDTO> getRevenueByDay() {
        return statisticsRepository.getRevenueByDay().stream()
                .map(p -> StatisticsResponseDTO.builder()
                        .reportDate(p.getReportDate().toString())
                        .totalOrders(p.getTotalOrders())
                        .totalProductsSold(p.getTotalProductsSold())
                        .totalRevenue(p.getTotalRevenue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StatisticsResponseDTO> getRevenueByMonth() {
        return statisticsRepository.getRevenueByMonth().stream()
                .map(p -> StatisticsResponseDTO.builder()
                        .reportYear(p.getReportYear())
                        .reportMonth(p.getReportMonth())
                        .totalOrders(p.getTotalOrders())
                        .totalProductsSold(p.getTotalProductsSold())
                        .totalRevenue(p.getTotalRevenue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopProductResponseDTO> getTopSellingProducts(int topN) {
        return statisticsRepository.getTopSellingProducts(topN).stream()
                .map(p -> TopProductResponseDTO.builder()
                        .productName(p.getProductName())
                        .totalSold(p.getTotalSold())
                        .revenue(p.getRevenue())
                        .build())
                .collect(Collectors.toList());
    }

    //Đơn hàng theo thứ trong tuần
    @Transactional(readOnly = true)
    public List<OrderByWeekdayResponseDTO> getOrdersByWeekday() {
        return statisticsRepository.getOrdersByWeekday().stream()
                .map(p -> OrderByWeekdayResponseDTO.builder()
                        .dayLabel(WEEKDAY_LABELS[p.getDayOfWeek()])
                        .totalOrders(p.getTotalOrders())
                        .build())
                .collect(Collectors.toList());
    }

    //Doanh thu theo thương hiệu iphone, samsung, xiaomi, oppo
    @Transactional(readOnly = true)
    public List<BrandRevenueResponseDTO> getRevenueByBrand() {
        List<com.fpoly.marcusstore.repository.statistics.BrandRevenueProjection> raw =
                statisticsRepository.getRevenueByBrand();

        BigDecimal totalRevenue = raw.stream()
                .map(com.fpoly.marcusstore.repository.statistics.BrandRevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream()
                .map(p -> {
                    double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? p.getRevenue().multiply(BigDecimal.valueOf(100))
                                    .divide(totalRevenue, 1, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return BrandRevenueResponseDTO.builder()
                            .brand(p.getBrand())
                            .totalSold(p.getTotalSold())
                            .revenue(p.getRevenue())
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    //Sản phẩm sắp hết / hết hàng
    @Transactional(readOnly = true)
    public List<LowStockResponseDTO> getLowStockProducts() {
        return statisticsRepository.getLowStockProducts(LOW_STOCK_THRESHOLD).stream()
                .map(p -> LowStockResponseDTO.builder()
                        .skuCode(p.getSkuCode())
                        .productName(p.getProductName())
                        .brand(p.getBrand())
                        .stockQuantity(p.getStockQuantity())
                        .status(p.getStockQuantity() == 0 ? "Hết hàng" : "Sắp hết hàng")
                        .build())
                .collect(Collectors.toList());
    }

    // Khách hàng mua nhiều nhất
    @Transactional(readOnly = true)
    public List<TopCustomerResponseDTO> getTopCustomers(int topN) {
        List<com.fpoly.marcusstore.repository.shopping.TopCustomerProjection> raw =
                statisticsRepository.getTopCustomers(topN);

        BigDecimal totalSpentAll = raw.stream()
                .map(com.fpoly.marcusstore.repository.shopping.TopCustomerProjection::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return raw.stream()
                .map(p -> {
                    double contribution = totalSpentAll.compareTo(BigDecimal.ZERO) > 0
                            ? p.getTotalSpent().multiply(BigDecimal.valueOf(100))
                                    .divide(totalSpentAll, 1, RoundingMode.HALF_UP)
                                    .doubleValue()
                            : 0.0;
                    return TopCustomerResponseDTO.builder()
                            .customerName(p.getCustomerName())
                            .email(p.getEmail())
                            .totalOrders(p.getTotalOrders())
                            .totalSpent(p.getTotalSpent())
                            .contributionPercent(contribution)
                            .build();
                })
                .collect(Collectors.toList());
    }

    //Đơn hàng gần nhất
    @Transactional(readOnly = true)
    public List<RecentOrderResponseDTO> getRecentOrders(int limit) {
        return statisticsRepository.getRecentOrders(limit).stream()
                .map(p -> RecentOrderResponseDTO.builder()
                        .orderCode(p.getOrderCode())
                        .customerName(p.getCustomerName())
                        .phone(p.getPhone())
                        .paymentMethod(p.getPaymentMethod())
                        .orderStatus(p.getOrderStatus())
                        .totalAmount(p.getTotalAmount())
                        .createdAt(p.getCreatedAt().toLocalDateTime())
                        .build())
                .collect(Collectors.toList());
    }
}