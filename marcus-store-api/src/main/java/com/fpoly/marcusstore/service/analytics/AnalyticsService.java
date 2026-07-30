package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.dto.analytics.AnalyticsMetric;
import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.dto.analytics.AnalyticsRateMetric;
import com.fpoly.marcusstore.dto.analytics.AnalyticsTrendPoint;
import com.fpoly.marcusstore.dto.analytics.ProductTrendResponse;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.SalesSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final int DEFAULT_NUMBER_OF_DAYS = 30;
    private static final int MAX_NUMBER_OF_DAYS = 1_826;
    private static final int MAX_PRODUCT_LIMIT = 100;

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsOverviewResponse getOverview(LocalDate fromDate, LocalDate toDate) {
        AnalyticsPeriod period = resolvePeriod(fromDate, toDate);
        SalesSummary current = loadSummary(period.fromDate(), period.toDate());
        SalesSummary previous = loadSummary(period.previousFromDate(), period.previousToDate());

        return new AnalyticsOverviewResponse(
                period,
                metric(current.completedSales(), previous.completedSales()),
                metric(current.completedOrders(), previous.completedOrders()),
                metric(current.unitsSold(), previous.unitsSold()),
                metric(current.averageOrderValue(), previous.averageOrderValue()),
                rateMetric(current.completionRate(), previous.completionRate()),
                rateMetric(current.cancellationRate(), previous.cancellationRate()),
                metric(current.successfulRefundAmount(), previous.successfulRefundAmount()),
                metric(current.orderingCustomers(), previous.orderingCustomers()));
    }

    public List<AnalyticsTrendPoint> getSalesTrend(LocalDate fromDate, LocalDate toDate) {
        AnalyticsPeriod period = resolvePeriod(fromDate, toDate);
        LocalDateTime from = period.fromDate().atStartOfDay();
        LocalDateTime toExclusive = period.toDate().plusDays(1).atStartOfDay();

        return analyticsRepository.findSalesTrend(
                period.fromDate(), period.toDate(), from, toExclusive)
                .stream()
                .map(point -> new AnalyticsTrendPoint(
                        point.getReportDate(),
                        zero(point.getCompletedSales()),
                        value(point.getCompletedOrders()),
                        value(point.getUnitsSold())))
                .toList();
    }

    public List<ProductTrendResponse> getProductTrends(
            LocalDate fromDate,
            LocalDate toDate,
            int limit) {
        AnalyticsPeriod period = resolvePeriod(fromDate, toDate);
        int safeLimit = Math.max(1, Math.min(limit, MAX_PRODUCT_LIMIT));

        return analyticsRepository.findProductTrends(
                period.fromDate().atStartOfDay(),
                period.toDate().plusDays(1).atStartOfDay(),
                period.previousFromDate().atStartOfDay(),
                period.previousToDate().plusDays(1).atStartOfDay())
                .stream()
                .limit(safeLimit)
                .map(product -> new ProductTrendResponse(
                        product.getProductId(),
                        product.getProductName(),
                        product.getBrand(),
                        value(product.getCurrentUnits()),
                        value(product.getPreviousUnits()),
                        calculateChange(value(product.getCurrentUnits()), value(product.getPreviousUnits())),
                        zero(product.getCurrentMerchandiseSales()),
                        zero(product.getPreviousMerchandiseSales())))
                .toList();
    }

    /**
     * Marcus thêm: kỳ mặc định là 30 ngày tính cả hôm nay; kỳ so sánh nằm ngay
     * trước đó và luôn có cùng số ngày.
     */
    public AnalyticsPeriod resolvePeriod(LocalDate fromDate, LocalDate toDate) {
        LocalDate resolvedTo = toDate != null ? toDate : LocalDate.now();
        LocalDate resolvedFrom = fromDate != null
                ? fromDate
                : resolvedTo.minusDays(DEFAULT_NUMBER_OF_DAYS - 1L);

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("Từ ngày không được lớn hơn đến ngày");
        }

        long numberOfDays = ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;
        if (numberOfDays > MAX_NUMBER_OF_DAYS) {
            throw new IllegalArgumentException("Khoảng phân tích không được vượt quá 5 năm");
        }

        LocalDate previousTo = resolvedFrom.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(numberOfDays - 1);
        return new AnalyticsPeriod(
                resolvedFrom,
                resolvedTo,
                previousFrom,
                previousTo,
                numberOfDays);
    }

    private SalesSummary loadSummary(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate.plusDays(1).atStartOfDay();
        SalesSummaryProjection projection = analyticsRepository.summarizeSales(from, toExclusive);
        BigDecimal refunds = zero(analyticsRepository.sumSuccessfulRefunds(from, toExclusive));

        return new SalesSummary(
                value(projection.getTotalOrders()),
                value(projection.getCompletedOrders()),
                value(projection.getCancelledOrders()),
                zero(projection.getCompletedSales()),
                value(projection.getUnitsSold()),
                value(projection.getOrderingCustomers()),
                refunds);
    }

    private static AnalyticsMetric metric(BigDecimal current, BigDecimal previous) {
        return new AnalyticsMetric(current, previous, calculateChange(current, previous));
    }

    private static AnalyticsMetric metric(long current, long previous) {
        return metric(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private static AnalyticsRateMetric rateMetric(double current, double previous) {
        return new AnalyticsRateMetric(
                round(current),
                round(previous),
                round(current - previous));
    }

    private static Double calculateChange(long current, long previous) {
        return calculateChange(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private static Double calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) {
            return current.signum() == 0 ? 0D : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static long value(Long value) {
        return value != null ? value : 0L;
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record SalesSummary(
            long totalOrders,
            long completedOrders,
            long cancelledOrders,
            BigDecimal completedSales,
            long unitsSold,
            long orderingCustomers,
            BigDecimal successfulRefundAmount) {

        BigDecimal averageOrderValue() {
            if (completedOrders == 0) {
                return BigDecimal.ZERO;
            }
            return completedSales.divide(
                    BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP);
        }

        double completionRate() {
            return percentage(completedOrders, totalOrders);
        }

        double cancellationRate() {
            return percentage(cancelledOrders, totalOrders);
        }

        private static double percentage(long part, long total) {
            if (total == 0) {
                return 0D;
            }
            return BigDecimal.valueOf(part)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }
}
