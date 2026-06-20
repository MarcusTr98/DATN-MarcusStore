package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.repository.statistics.RevenueCompareProjection;
import com.fpoly.marcusstore.repository.statistics.StatisticsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private StatisticsRepository statisticsRepository;

    private static final String[] WEEKDAY_LABELS = {
            "", "Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"
    };
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    public static LocalDate[] resolveDateRange(LocalDate startDate, LocalDate endDate, String period) {
        if (startDate != null && endDate != null) {
            return new LocalDate[]{startDate, endDate};
        }
        LocalDate now = LocalDate.now();
        LocalDate start;
        LocalDate end = now;

        switch (period == null ? "month" : period) {
            case "today"   -> start = now;
            case "week"    -> start = now.minusDays(6);
            case "quarter" -> {
                int firstMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
                start = now.withMonth(firstMonth).withDayOfMonth(1);
            }
            case "year"    -> start = now.withDayOfYear(1);
            default        -> start = now.withDayOfMonth(1); // month
        }
        return new LocalDate[]{start, end};
    }

    @Transactional(readOnly = true)
    public List<StatisticsResponseDTO> getRevenueByDay(LocalDate startDate, LocalDate endDate) {
        return statisticsRepository.getRevenueByDay(startDate, endDate).stream()
                .map(p -> StatisticsResponseDTO.builder()
                        .reportDate(p.getReportDate().toString())
                        .totalOrders(p.getTotalOrders())
                        .totalProductsSold(p.getTotalProductsSold())
                        .totalRevenue(p.getTotalRevenue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StatisticsResponseDTO> getRevenueByMonth(Integer year) {
        return statisticsRepository.getRevenueByMonth(year).stream()
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
    public List<TopProductResponseDTO> getTopSellingProducts(int topN, LocalDate startDate, LocalDate endDate) {
        return statisticsRepository.getTopSellingProducts(topN, startDate, endDate).stream()
                .map(p -> TopProductResponseDTO.builder()
                        .productName(p.getProductName())
                        .totalSold(p.getTotalSold())
                        .revenue(p.getRevenue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderByWeekdayResponseDTO> getOrdersByWeekday(LocalDate startDate, LocalDate endDate) {
        return statisticsRepository.getOrdersByWeekday(startDate, endDate).stream()
                .map(p -> OrderByWeekdayResponseDTO.builder()
                        .dayLabel(WEEKDAY_LABELS[p.getDayOfWeek()])
                        .totalOrders(p.getTotalOrders())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BrandRevenueResponseDTO> getRevenueByBrand(LocalDate startDate, LocalDate endDate) {
        List<com.fpoly.marcusstore.repository.statistics.BrandRevenueProjection> raw =
                statisticsRepository.getRevenueByBrand(startDate, endDate);

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

    @Transactional(readOnly = true)
    public List<TopCustomerResponseDTO> getTopCustomers(int topN, LocalDate startDate, LocalDate endDate) {
        List<com.fpoly.marcusstore.repository.shopping.TopCustomerProjection> raw =
                statisticsRepository.getTopCustomers(topN, startDate, endDate);

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

    @Transactional(readOnly = true)
    public List<RecentOrderResponseDTO> getRecentOrders(int limit, LocalDate startDate, LocalDate endDate) {
        return statisticsRepository.getRecentOrders(limit, startDate, endDate).stream()
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

    //So sánh doanh thu kỳ này với kỳ trước
    @Transactional(readOnly = true)
    public RevenueCompareResponseDTO getRevenueCompare(String period) {
        LocalDate today = LocalDate.now();
        LocalDate currentStart, currentEnd, previousStart, previousEnd;
        String currentLabel, previousLabel;

        switch (period == null ? "month" : period) {
            case "today" -> {
                currentStart  = today;
                currentEnd    = today;
                previousStart = today.minusDays(1);
                previousEnd   = today.minusDays(1);
                currentLabel  = "Hôm nay (" + today.format(SHORT_DATE) + ")";
                previousLabel = "Hôm qua (" + today.minusDays(1).format(SHORT_DATE) + ")";
            }
            case "week" -> {
                currentStart  = today.minusDays(6);
                currentEnd    = today;
                previousStart = today.minusDays(13);
                previousEnd   = today.minusDays(7);
                currentLabel  = "Tuần này";
                previousLabel = "Tuần trước";
            }
            case "year" -> {
                currentStart  = today.withDayOfYear(1);
                currentEnd    = today;
                previousStart = today.minusYears(1).withDayOfYear(1);
                previousEnd   = today.minusYears(1);
                currentLabel  = "Năm " + today.getYear();
                previousLabel = "Năm " + (today.getYear() - 1);
            }
            default -> { // month
                currentStart  = today.withDayOfMonth(1);
                currentEnd    = today;
                previousStart = today.minusMonths(1).withDayOfMonth(1);
                previousEnd   = today.minusMonths(1)
                        .withDayOfMonth(today.minusMonths(1).lengthOfMonth());
                currentLabel  = "Tháng " + today.getMonthValue() + "/" + today.getYear();
                previousLabel = "Tháng " + today.minusMonths(1).getMonthValue()
                        + "/" + today.minusMonths(1).getYear();
            }
        }

        List<RevenueCompareProjection> currentRaw =
                statisticsRepository.getRevenueByDateRange(currentStart, currentEnd);
        List<RevenueCompareProjection> previousRaw =
                statisticsRepository.getRevenueByDateRange(previousStart, previousEnd);

        return RevenueCompareResponseDTO.builder()
                .current(groupByPeriod(currentRaw, period, currentStart))
                .previous(groupByPeriod(previousRaw, period, previousStart))
                .currentLabel(currentLabel)
                .previousLabel(previousLabel)
                .build();
    }

    private List<RevenueCompareResponseDTO.PeriodData> groupByPeriod(
        List<RevenueCompareProjection> raw, String period, LocalDate periodStart) {

    if ("today".equals(period) || "week".equals(period)) {
        return raw.stream()
                .map(p -> RevenueCompareResponseDTO.PeriodData.builder()
                        .label(LocalDate.parse(p.getDateLabel().toString())
                                .format(SHORT_DATE))
                        .revenue(p.getTotalRevenue().doubleValue())
                        .build())
                .collect(Collectors.toList());
    } else {
        // Nhóm theo tuần, label dạng "01/05 - 07/05"
        Map<Integer, Double> weekRevenueMap = new LinkedHashMap<>();
        Map<Integer, LocalDate> weekStartMap = new LinkedHashMap<>();

        for (RevenueCompareProjection p : raw) {
            LocalDate date = LocalDate.parse(p.getDateLabel().toString());
            int weekNum = (int)(ChronoUnit.DAYS.between(periodStart, date) / 7);
            weekRevenueMap.merge(weekNum, p.getTotalRevenue().doubleValue(), Double::sum);
            weekStartMap.putIfAbsent(weekNum, date);
        }

        return weekRevenueMap.entrySet().stream()
                .map(e -> {
                    LocalDate weekStart = weekStartMap.get(e.getKey());
                    LocalDate weekEnd = weekStart.plusDays(6);
                    String label = weekStart.format(SHORT_DATE) + " - " + weekEnd.format(SHORT_DATE);
                    return RevenueCompareResponseDTO.PeriodData.builder()
                            .label(label)
                            .revenue(e.getValue())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
public Long countPendingOrders() {
    return statisticsRepository.countPendingOrders();
}
}