package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.repository.statistics.BrandRevenueProjection;
import com.fpoly.marcusstore.repository.statistics.NewUserByDayProjection;
import com.fpoly.marcusstore.repository.statistics.RevenueCompareProjection;
import com.fpoly.marcusstore.repository.statistics.StatisticsRepository;
import com.fpoly.marcusstore.repository.shopping.TopCustomerProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
            case "today"     -> start = now;
            case "yesterday" -> { start = now.minusDays(1); end = now.minusDays(1); }
            case "7days"     -> start = now.minusDays(6);
            case "30days"    -> start = now.minusDays(29);
            // FIX #5: dùng end = now thay vì start.plusDays(6)
            // để không truy vấn các ngày tương lai trong tuần
            case "week"      -> { start = now.with(DayOfWeek.MONDAY); end = start.with(DayOfWeek.SUNDAY); }
            case "quarter"   -> {
                int firstMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
                start = now.withMonth(firstMonth).withDayOfMonth(1);
            }
            case "year"      -> start = now.withDayOfYear(1);
            default          -> {
                start = now.withDayOfMonth(1);
                end   = now.withDayOfMonth(now.lengthOfMonth());
            }
        }
        return new LocalDate[]{start, end};
    }

    private static LocalDate toLocalDate(Object raw) {
        if (raw == null) return null;
        if (raw instanceof LocalDate) return (LocalDate) raw;
        if (raw instanceof java.sql.Date) return ((java.sql.Date) raw).toLocalDate();
        return LocalDate.parse(raw.toString());
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
    public KpiSummaryDTO getKpiSummary(LocalDate startDate, LocalDate endDate) {
        var p = statisticsRepository.getKpiSummary(startDate, endDate);
        return KpiSummaryDTO.builder()
                .totalRevenue(p.getTotalRevenue() != null ? p.getTotalRevenue() : BigDecimal.ZERO)
                .totalOrders(p.getTotalOrders() != null ? p.getTotalOrders() : 0L)
                .totalProductsSold(p.getTotalProductsSold() != null ? p.getTotalProductsSold() : 0L)
                .build();
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
    public List<TopProductResponseDTO> getTopSellingProducts(
            int topN, LocalDate startDate, LocalDate endDate, String keyword) {
        return statisticsRepository.getTopSellingProducts(topN, startDate, endDate,
                        toNullIfBlank(keyword)).stream()
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
        List<BrandRevenueProjection> raw = statisticsRepository.getRevenueByBrand(startDate, endDate);
        BigDecimal totalRevenue = raw.stream()
                .map(BrandRevenueProjection::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return raw.stream()
                .map(p -> {
                    double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? p.getRevenue().multiply(BigDecimal.valueOf(100))
                                    .divide(totalRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return BrandRevenueResponseDTO.builder()
                            .brand(p.getBrand()).totalSold(p.getTotalSold())
                            .revenue(p.getRevenue()).percentage(percentage).build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LowStockResponseDTO> getLowStockProducts(String keyword, String brand, String status) {
        return statisticsRepository.getLowStockProducts(
                        LOW_STOCK_THRESHOLD, toNullIfBlank(keyword),
                        toNullIfBlank(brand), toNullIfBlank(status)).stream()
                .map(p -> LowStockResponseDTO.builder()
                        .skuCode(p.getSkuCode()).productName(p.getProductName()).brand(p.getBrand())
                        .stockQuantity(p.getStockQuantity())
                        .status(p.getStockQuantity() == 0 ? "Hết hàng" : "Sắp hết hàng").build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopCustomerResponseDTO> getTopCustomers(
            int topN, LocalDate startDate, LocalDate endDate, String keyword) {
        List<TopCustomerProjection> raw =
                statisticsRepository.getTopCustomers(topN, startDate, endDate, toNullIfBlank(keyword));
        BigDecimal totalShopRevenue = statisticsRepository.getTotalRevenue(startDate, endDate);
        if (totalShopRevenue == null) totalShopRevenue = BigDecimal.ZERO;
        final BigDecimal denominator = totalShopRevenue;
        return raw.stream()
                .map(p -> {
                    double contribution = denominator.compareTo(BigDecimal.ZERO) > 0
                            ? p.getTotalSpent().multiply(BigDecimal.valueOf(100))
                                    .divide(denominator, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return TopCustomerResponseDTO.builder()
                            .customerName(p.getCustomerName()).email(p.getEmail())
                            .totalOrders(p.getTotalOrders()).totalSpent(p.getTotalSpent())
                            .contributionPercent(contribution).build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecentOrderResponseDTO> getRecentOrders(
            int limit, LocalDate startDate, LocalDate endDate,
            String keyword, String status, String brand) {
        return statisticsRepository.getRecentOrders(
                        limit, startDate, endDate,
                        toNullIfBlank(keyword), toNullIfBlank(status), toNullIfBlank(brand)).stream()
                .map(p -> RecentOrderResponseDTO.builder()
                        .orderCode(p.getOrderCode()).customerName(p.getCustomerName())
                        .phone(p.getPhone()).paymentMethod(p.getPaymentMethod())
                        .orderStatus(p.getOrderStatus()).totalAmount(p.getTotalAmount())
                        .createdAt(p.getCreatedAt().toLocalDateTime()).build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RevenueCompareResponseDTO getRevenueCompare(String period, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate currentStart, currentEnd, previousStart, previousEnd;
        String currentLabel, previousLabel;

        if (startDate != null && endDate != null) {
            currentStart = startDate;
            currentEnd   = endDate;
            long days    = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
            previousStart = startDate.minusDays(days);
            previousEnd   = endDate.minusDays(days);
            currentLabel  = startDate.equals(endDate)
                    ? startDate.format(SHORT_DATE)
                    : startDate.format(SHORT_DATE) + "–" + endDate.format(SHORT_DATE);
            previousLabel = previousStart.format(SHORT_DATE) + "–" + previousEnd.format(SHORT_DATE);

            List<RevenueCompareProjection> currentRaw =
                    statisticsRepository.getRevenueByDateRange(currentStart, currentEnd);
            List<RevenueCompareProjection> previousRaw =
                    statisticsRepository.getRevenueByDateRange(previousStart, previousEnd);
            return RevenueCompareResponseDTO.builder()
                    .current(groupByPeriod(currentRaw, "custom", currentStart))
                    .previous(groupByPeriod(previousRaw, "custom", previousStart))
                    .currentLabel(currentLabel)
                    .previousLabel(previousLabel)
                    .build();
        }

        switch (period == null ? "month" : period) {
            case "today" -> {
                currentStart  = today;
                currentEnd    = today;
                previousStart = today.minusDays(1);
                previousEnd   = today.minusDays(1);
                currentLabel  = "Hôm nay (" + today.format(SHORT_DATE) + ")";
                previousLabel = "Hôm qua (" + today.minusDays(1).format(SHORT_DATE) + ")";
            }
            case "yesterday" -> {
                currentStart  = today.minusDays(1);
                currentEnd    = today.minusDays(1);
                previousStart = today.minusDays(2);
                previousEnd   = today.minusDays(2);
                currentLabel  = "Hôm qua (" + today.minusDays(1).format(SHORT_DATE) + ")";
                previousLabel = "Hôm kia (" + today.minusDays(2).format(SHORT_DATE) + ")";
            }
            case "7days" -> {
                currentStart  = today.minusDays(6);
                currentEnd    = today;
                previousStart = today.minusDays(13);
                previousEnd   = today.minusDays(7);
                currentLabel  = "7 ngày qua (" + today.minusDays(6).format(SHORT_DATE)
                        + "–" + today.format(SHORT_DATE) + ")";
                previousLabel = "7 ngày trước (" + today.minusDays(13).format(SHORT_DATE)
                        + "–" + today.minusDays(7).format(SHORT_DATE) + ")";
            }
            case "30days" -> {
                currentStart  = today.minusDays(29);
                currentEnd    = today;
                previousStart = today.minusDays(59);
                previousEnd   = today.minusDays(30);
                currentLabel  = "30 ngày qua (" + today.minusDays(29).format(SHORT_DATE)
                        + "–" + today.format(SHORT_DATE) + ")";
                previousLabel = "30 ngày trước (" + today.minusDays(59).format(SHORT_DATE)
                        + "–" + today.minusDays(30).format(SHORT_DATE) + ")";
            }
            case "week" -> {
                currentStart  = today.with(DayOfWeek.MONDAY);
                currentEnd    = today;
                previousStart = currentStart.minusDays(7);
                previousEnd   = today.minusDays(7);
                currentLabel  = "Tuần này ("
                        + currentStart.format(SHORT_DATE) + "–" + currentEnd.format(SHORT_DATE) + ")";
                previousLabel = "Tuần trước ("
                        + previousStart.format(SHORT_DATE) + "–" + previousEnd.format(SHORT_DATE) + ")";
            }
            case "year" -> {
                currentStart  = today.withDayOfYear(1);
                currentEnd    = today;
                previousStart = today.minusYears(1).withDayOfYear(1);
                previousEnd   = today.minusYears(1);
                currentLabel  = "Năm " + today.getYear();
                previousLabel = "Năm " + (today.getYear() - 1);
            }
            default -> {  // month
                currentStart  = today.withDayOfMonth(1);
                currentEnd    = today;
                previousStart = today.minusMonths(1).withDayOfMonth(1);
                int lastDay   = today.minusMonths(1).lengthOfMonth();
                previousEnd   = today.minusMonths(1).withDayOfMonth(
                        Math.min(today.getDayOfMonth(), lastDay));
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

    @Transactional(readOnly = true)
    public List<NewUserStatsDTO> getNewUsers(String period, LocalDate startDate, LocalDate endDate) {
        List<NewUserByDayProjection> raw = statisticsRepository.getNewUsersByDay(startDate, endDate);
        if ("year".equals(period)) {
            Map<Integer, Long> monthMap = new LinkedHashMap<>();
            for (int m = 1; m <= 12; m++) monthMap.put(m, 0L);
            for (var p : raw) {
                LocalDate date = toLocalDate(p.getRegisterDate());
                if (date != null) {
                    monthMap.merge(date.getMonthValue(), p.getTotalNewUsers(), Long::sum);
                }
            }
            int year = startDate.getYear();
            return monthMap.entrySet().stream()
                    .map(e -> NewUserStatsDTO.builder()
                            .registerDate(String.format("T%d/%d", e.getKey(), year))
                            .totalNewUsers(e.getValue()).build())
                    .collect(Collectors.toList());
        } else {
            return raw.stream()
                    .map(p -> NewUserStatsDTO.builder()
                            .registerDate(p.getRegisterDate().toString())
                            .totalNewUsers(p.getTotalNewUsers()).build())
                    .collect(Collectors.toList());
        }
    }

    public Long countPendingOrders() {
        return statisticsRepository.countPendingOrders();
    }

    private String toNullIfBlank(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String toWeekdayLabel(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY    -> "T2";
            case TUESDAY   -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY  -> "T5";
            case FRIDAY    -> "T6";
            case SATURDAY  -> "T7";
            case SUNDAY    -> "CN";
        };
    }

    private List<RevenueCompareResponseDTO.PeriodData> groupByPeriod(
            List<RevenueCompareProjection> raw, String period, LocalDate periodStart) {

        if (List.of("today", "yesterday", "week", "7days", "30days", "custom").contains(period)) {
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM");
            return raw.stream()
                    .map(p -> {
                        LocalDate date = toLocalDate(p.getDateLabel());
                        String label = "week".equals(period)
                                ? toWeekdayLabel(date)
                                : date.format(dateFmt);
                        return RevenueCompareResponseDTO.PeriodData.builder()
                                .label(label)
                                .sublabel(date.format(dateFmt))
                                .revenue(p.getTotalRevenue().doubleValue())
                                .build();
                    })
                    .collect(Collectors.toList());

        } else if ("month".equals(period)) {
            Map<Integer, Double>    dayRevenueMap = new LinkedHashMap<>();
            Map<Integer, LocalDate> dayDateMap    = new LinkedHashMap<>();
            for (RevenueCompareProjection p : raw) {
                LocalDate date = toLocalDate(p.getDateLabel());
                if (date == null) continue;
                int day = date.getDayOfMonth();
                dayRevenueMap.merge(day, p.getTotalRevenue().doubleValue(), Double::sum);
                dayDateMap.putIfAbsent(day, date);
            }
            return dayRevenueMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> RevenueCompareResponseDTO.PeriodData.builder()
                            .label("Ngày " + e.getKey())
                            .sublabel(dayDateMap.get(e.getKey()).format(SHORT_DATE))
                            .revenue(e.getValue()).build())
                    .collect(Collectors.toList());

        } else {
            Map<Integer, Double>  monthRevenueMap = new LinkedHashMap<>();
            Map<Integer, Integer> monthYearMap    = new LinkedHashMap<>();
            for (int m = 1; m <= 12; m++) {
                monthRevenueMap.put(m, 0.0);
                monthYearMap.put(m, periodStart.getYear());
            }
            for (RevenueCompareProjection p : raw) {
                // FIX #8: parse an toàn
                LocalDate date = toLocalDate(p.getDateLabel());
                if (date == null) continue;
                int month = date.getMonthValue();
                monthRevenueMap.merge(month, p.getTotalRevenue().doubleValue(), Double::sum);
                monthYearMap.put(month, date.getYear());
            }
            return monthRevenueMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> {
                        int m  = e.getKey();
                        int yr = monthYearMap.getOrDefault(m, periodStart.getYear());
                        return RevenueCompareResponseDTO.PeriodData.builder()
                                .label("T" + m)
                                .sublabel(String.format("%02d/%d", m, yr))
                                .revenue(e.getValue()).build();
                    })
                    .collect(Collectors.toList());
        }
    }
}