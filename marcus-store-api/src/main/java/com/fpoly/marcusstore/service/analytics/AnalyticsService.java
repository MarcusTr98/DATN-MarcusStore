package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.dto.analytics.AnalyticsMetric;
import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.dto.analytics.AnalyticsRateMetric;
import com.fpoly.marcusstore.dto.analytics.AnalyticsTrendPoint;
import com.fpoly.marcusstore.dto.analytics.ProductTrendResponse;
import com.fpoly.marcusstore.dto.analytics.CancellationReasonResponse;
import com.fpoly.marcusstore.dto.analytics.ProductWarrantyQualityResponse;
import com.fpoly.marcusstore.dto.analytics.WarrantyAnalyticsResponse;
import com.fpoly.marcusstore.dto.analytics.WarrantyReasonMetric;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.CancellationReasonProjection;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<CancellationReasonResponse> getCancellationReasons(
            LocalDate fromDate,
            LocalDate toDate) {
        AnalyticsPeriod period = resolvePeriod(fromDate, toDate);
        Map<String, Long> current = loadCancellationReasons(period.fromDate(), period.toDate());
        Map<String, Long> previous = loadCancellationReasons(
                period.previousFromDate(), period.previousToDate());
        long currentTotal = current.values().stream().mapToLong(Long::longValue).sum();

        LinkedHashSet<String> reasons = new LinkedHashSet<>(current.keySet());
        reasons.addAll(previous.keySet());
        return reasons.stream()
                .map(reason -> {
                    long currentCount = current.getOrDefault(reason, 0L);
                    long previousCount = previous.getOrDefault(reason, 0L);
                    return new CancellationReasonResponse(
                            reason,
                            currentCount,
                            previousCount,
                            currentTotal == 0 ? 0D : round(currentCount * 100D / currentTotal),
                            calculateChange(currentCount, previousCount));
                })
                .sorted((left, right) -> Long.compare(right.currentCount(), left.currentCount()))
                .toList();
    }

    /**
     * Marcus thêm: chỉ số bảo hành dùng ngày khách tạo yêu cầu. Đây là tín hiệu
     * hậu mãi/chất lượng cần theo dõi, không tự gọi là tỷ lệ lỗi sản phẩm vì yêu
     * cầu có thể phát sinh sau kỳ bán hàng.
     */
    public WarrantyAnalyticsResponse getWarrantyAnalytics(
            LocalDate fromDate,
            LocalDate toDate,
            int productLimit) {
        AnalyticsPeriod period = resolvePeriod(fromDate, toDate);
        WarrantySummary current = loadWarrantySummary(period.fromDate(), period.toDate());
        WarrantySummary previous = loadWarrantySummary(
                period.previousFromDate(), period.previousToDate());
        int safeLimit = Math.max(1, Math.min(productLimit, MAX_PRODUCT_LIMIT));

        List<WarrantyReasonMetric> reasons = loadWarrantyReasons(
                period.fromDate(), period.toDate(), current.totalRequests());
        List<ProductWarrantyQualityResponse> products = analyticsRepository
                .findProductWarrantyQuality(
                        period.fromDate().atStartOfDay(),
                        period.toDate().plusDays(1).atStartOfDay(),
                        period.previousFromDate().atStartOfDay(),
                        period.previousToDate().plusDays(1).atStartOfDay())
                .stream()
                .limit(safeLimit)
                .map(product -> {
                    long approved = value(product.getApprovedRequests());
                    long rejected = value(product.getRejectedRequests());
                    return new ProductWarrantyQualityResponse(
                            product.getProductId(),
                            product.getProductName(),
                            product.getBrand(),
                            value(product.getCurrentRequests()),
                            value(product.getPreviousRequests()),
                            calculateChange(
                                    value(product.getCurrentRequests()),
                                    value(product.getPreviousRequests())),
                            approved,
                            rejected,
                            percentage(approved, approved + rejected));
                })
                .toList();

        return new WarrantyAnalyticsResponse(
                period,
                metric(current.totalRequests(), previous.totalRequests()),
                rateMetric(current.resolutionRate(), previous.resolutionRate()),
                rateMetric(current.approvalRate(), previous.approvalRate()),
                current.pendingRequests(),
                current.processingRequests(),
                current.approvedRequests(),
                current.rejectedRequests(),
                reasons,
                products);
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
                value(projection.getCohortCompletedOrders()),
                value(projection.getCancelledOrders()),
                zero(projection.getCompletedSales()),
                value(projection.getUnitsSold()),
                value(projection.getOrderingCustomers()),
                refunds);
    }

    private Map<String, Long> loadCancellationReasons(LocalDate fromDate, LocalDate toDate) {
        return analyticsRepository.findCancellationReasons(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.toMap(
                        CancellationReasonProjection::getReason,
                        reason -> value(reason.getReasonCount()),
                        Long::sum,
                        java.util.LinkedHashMap::new));
    }

    private WarrantySummary loadWarrantySummary(LocalDate fromDate, LocalDate toDate) {
        var summary = analyticsRepository.summarizeWarranties(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());
        return new WarrantySummary(
                value(summary.getTotalRequests()),
                value(summary.getPendingRequests()),
                value(summary.getProcessingRequests()),
                value(summary.getApprovedRequests()),
                value(summary.getRejectedRequests()));
    }

    private List<WarrantyReasonMetric> loadWarrantyReasons(
            LocalDate fromDate,
            LocalDate toDate,
            long totalRequests) {
        return analyticsRepository.findWarrantyReasons(
                fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())
                .stream()
                .map(reason -> new WarrantyReasonMetric(
                        reason.getReason(),
                        warrantyReasonLabel(reason.getReason()),
                        value(reason.getReasonCount()),
                        percentage(value(reason.getReasonCount()), totalRequests)))
                .toList();
    }

    private static String warrantyReasonLabel(String reason) {
        return switch (reason == null ? "" : reason.toUpperCase()) {
            case "DEFECTIVE" -> "Sản phẩm bị lỗi";
            case "DAMAGED" -> "Sản phẩm bị hư hỏng";
            case "WRONG_ITEM" -> "Giao sai sản phẩm";
            case "NOT_AS_DESCRIBED" -> "Không đúng mô tả";
            case "ACCESSORY_MISSING" -> "Thiếu phụ kiện";
            default -> "Lý do khác";
        };
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

    private static double percentage(long part, long total) {
        if (total == 0) {
            return 0D;
        }
        return round(part * 100D / total);
    }

    private record SalesSummary(
            long totalOrders,
            long completedOrders,
            long cohortCompletedOrders,
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
            return percentage(cohortCompletedOrders, totalOrders);
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

    private record WarrantySummary(
            long totalRequests,
            long pendingRequests,
            long processingRequests,
            long approvedRequests,
            long rejectedRequests) {

        double resolutionRate() {
            return percentage(approvedRequests + rejectedRequests, totalRequests);
        }

        double approvalRate() {
            return percentage(approvedRequests, approvedRequests + rejectedRequests);
        }
    }
}
