package com.fpoly.marcusstore.service.analytics;

import com.fpoly.marcusstore.dto.analytics.AnalyticsOverviewResponse;
import com.fpoly.marcusstore.dto.analytics.AnalyticsPeriod;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.SalesSummaryProjection;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.CancellationReasonProjection;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.ProductWarrantyProjection;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.WarrantyReasonProjection;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository.WarrantySummaryProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

        private AnalyticsRepository analyticsRepository;
        private AnalyticsService analyticsService;

        @BeforeEach
        void setUp() {
                analyticsRepository = mock(AnalyticsRepository.class);
                analyticsService = new AnalyticsService(analyticsRepository);
        }

        @Test
        void createsAnImmediatelyPreviousPeriodWithTheSameNumberOfDays() {
                AnalyticsPeriod period = analyticsService.resolvePeriod(
                                LocalDate.of(2026, 7, 1),
                                LocalDate.of(2026, 7, 30));

                // Marcus thêm: so sánh 30 ngày hiện tại với đúng 30 ngày liền trước.
                assertThat(period.numberOfDays()).isEqualTo(30);
                assertThat(period.previousFromDate()).isEqualTo(LocalDate.of(2026, 6, 1));
                assertThat(period.previousToDate()).isEqualTo(LocalDate.of(2026, 6, 30));
        }

        @Test
        void rejectsInvalidOrOverlyLongRanges() {
                assertThatThrownBy(() -> analyticsService.resolvePeriod(
                                LocalDate.of(2026, 7, 2),
                                LocalDate.of(2026, 7, 1)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Từ ngày");

                assertThatThrownBy(() -> analyticsService.resolvePeriod(
                                LocalDate.of(2020, 1, 1),
                                LocalDate.of(2026, 1, 1)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("5 năm");
        }

        @Test
        void calculatesSalesRatesAverageOrderAndRefundGrowthFromSeparateSummaries() {
                LocalDate currentFrom = LocalDate.of(2026, 7, 1);
                LocalDate currentTo = LocalDate.of(2026, 7, 30);
                LocalDate previousFrom = LocalDate.of(2026, 6, 1);
                LocalDate previousTo = LocalDate.of(2026, 6, 30);
                SalesSummaryProjection currentSummary = summary(100, 80, 10, "80000000", 120, 65);
                SalesSummaryProjection previousSummary = summary(80, 60, 8, "60000000", 90, 50);

                when(analyticsRepository.summarizeSales(
                                currentFrom.atStartOfDay(),
                                currentTo.plusDays(1).atStartOfDay()))
                                .thenReturn(currentSummary);
                when(analyticsRepository.summarizeSales(
                                previousFrom.atStartOfDay(),
                                previousTo.plusDays(1).atStartOfDay()))
                                .thenReturn(previousSummary);
                when(analyticsRepository.sumSuccessfulRefunds(
                                currentFrom.atStartOfDay(),
                                currentTo.plusDays(1).atStartOfDay()))
                                .thenReturn(new BigDecimal("5000000"));
                when(analyticsRepository.sumSuccessfulRefunds(
                                previousFrom.atStartOfDay(),
                                previousTo.plusDays(1).atStartOfDay()))
                                .thenReturn(new BigDecimal("2000000"));

                AnalyticsOverviewResponse overview = analyticsService.getOverview(currentFrom, currentTo);

                assertThat(overview.completedSales().currentValue()).isEqualByComparingTo("80000000");
                assertThat(overview.completedSales().changePercent()).isEqualTo(33.33);
                assertThat(overview.averageOrderValue().currentValue()).isEqualByComparingTo("1000000");
                assertThat(overview.completionRate().currentPercent()).isEqualTo(80);
                assertThat(overview.completionRate().percentagePointChange()).isEqualTo(5);
                assertThat(overview.cancellationRate().currentPercent()).isEqualTo(10);
                assertThat(overview.successfulRefundAmount().currentValue()).isEqualByComparingTo("5000000");
                assertThat(overview.successfulRefundAmount().changePercent()).isEqualTo(150);
                assertThat(overview.unitsSold().currentValue()).isEqualByComparingTo("120");
        }

        @Test
        void usesAnExclusiveUpperDateBoundaryForIndexFriendlyQueries() {
                LocalDate from = LocalDate.of(2026, 7, 1);
                LocalDate to = LocalDate.of(2026, 7, 7);
                when(analyticsRepository.findSalesTrend(
                                from,
                                to,
                                from.atStartOfDay(),
                                to.plusDays(1).atStartOfDay()))
                                .thenReturn(java.util.List.of());

                analyticsService.getSalesTrend(from, to);

                verify(analyticsRepository).findSalesTrend(
                                from,
                                to,
                                LocalDateTime.of(2026, 7, 1, 0, 0),
                                LocalDateTime.of(2026, 7, 8, 0, 0));
        }

        @Test
        void returnsUnknownGrowthWhenThePreviousValueIsZero() {
                LocalDate from = LocalDate.of(2026, 7, 1);
                LocalDate to = LocalDate.of(2026, 7, 1);
                LocalDate previous = LocalDate.of(2026, 6, 30);
                SalesSummaryProjection currentSummary = summary(1, 1, 0, "1000000", 1, 1);
                SalesSummaryProjection previousSummary = summary(0, 0, 0, "0", 0, 0);

                when(analyticsRepository.summarizeSales(
                                from.atStartOfDay(),
                                to.plusDays(1).atStartOfDay()))
                                .thenReturn(currentSummary);
                when(analyticsRepository.summarizeSales(
                                previous.atStartOfDay(),
                                from.atStartOfDay()))
                                .thenReturn(previousSummary);
                when(analyticsRepository.sumSuccessfulRefunds(
                                from.atStartOfDay(),
                                to.plusDays(1).atStartOfDay()))
                                .thenReturn(BigDecimal.ZERO);
                when(analyticsRepository.sumSuccessfulRefunds(
                                previous.atStartOfDay(),
                                from.atStartOfDay()))
                                .thenReturn(BigDecimal.ZERO);

                AnalyticsOverviewResponse overview = analyticsService.getOverview(from, to);

                // Marcus sửa: không hiển thị tăng vô hạn khi kỳ trước bằng 0.
                assertThat(overview.completedSales().changePercent()).isNull();
                assertThat(overview.successfulRefundAmount().changePercent()).isZero();
        }

        @Test
        void comparesNormalizedCancellationReasonsWithoutExposingFreeText() {
                LocalDate from = LocalDate.of(2026, 7, 1);
                LocalDate to = LocalDate.of(2026, 7, 30);
                CancellationReasonProjection current = cancellationReason("Không còn nhu cầu mua", 6);
                CancellationReasonProjection previous = cancellationReason("Không còn nhu cầu mua", 3);

                when(analyticsRepository.findCancellationReasons(
                                from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                                .thenReturn(java.util.List.of(current));
                when(analyticsRepository.findCancellationReasons(
                                LocalDate.of(2026, 6, 1).atStartOfDay(), from.atStartOfDay()))
                                .thenReturn(java.util.List.of(previous));

                var reasons = analyticsService.getCancellationReasons(from, to);

                assertThat(reasons).hasSize(1);
                assertThat(reasons.getFirst().reason()).isEqualTo("Không còn nhu cầu mua");
                assertThat(reasons.getFirst().sharePercent()).isEqualTo(100);
                assertThat(reasons.getFirst().changePercent()).isEqualTo(100);
        }

        @Test
        void summarizesWarrantyQualityWithoutReadingCustomerFreeText() {
                LocalDate from = LocalDate.of(2026, 7, 1);
                LocalDate to = LocalDate.of(2026, 7, 30);
                WarrantySummaryProjection current = warrantySummary(10, 2, 3, 4, 1);
                WarrantySummaryProjection previous = warrantySummary(5, 1, 1, 2, 1);
                WarrantyReasonProjection reason = mock(WarrantyReasonProjection.class);
                when(reason.getReason()).thenReturn("DEFECTIVE");
                when(reason.getReasonCount()).thenReturn(6L);
                ProductWarrantyProjection product = mock(ProductWarrantyProjection.class);
                when(product.getProductId()).thenReturn(15);
                when(product.getProductName()).thenReturn("iPhone Test");
                when(product.getBrand()).thenReturn("Apple");
                when(product.getCurrentRequests()).thenReturn(6L);
                when(product.getPreviousRequests()).thenReturn(3L);
                when(product.getApprovedRequests()).thenReturn(3L);
                when(product.getRejectedRequests()).thenReturn(1L);

                when(analyticsRepository.summarizeWarranties(
                                from.atStartOfDay(), to.plusDays(1).atStartOfDay())).thenReturn(current);
                when(analyticsRepository.summarizeWarranties(
                                LocalDate.of(2026, 6, 1).atStartOfDay(), from.atStartOfDay())).thenReturn(previous);
                when(analyticsRepository.findWarrantyReasons(
                                from.atStartOfDay(), to.plusDays(1).atStartOfDay()))
                                .thenReturn(java.util.List.of(reason));
                when(analyticsRepository.findProductWarrantyQuality(
                                from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                                LocalDate.of(2026, 6, 1).atStartOfDay(), from.atStartOfDay()))
                                .thenReturn(java.util.List.of(product));

                var result = analyticsService.getWarrantyAnalytics(from, to, 10);

                // Marcus kiểm tra: tỷ lệ xử lý = APPROVED + REJECTED; tỷ lệ đồng ý
                // chỉ dùng các yêu cầu đã có kết luận, không tính PENDING/CONFIRMED.
                assertThat(result.totalRequests().currentValue()).isEqualByComparingTo("10");
                assertThat(result.resolutionRate().currentPercent()).isEqualTo(50);
                assertThat(result.approvalRate().currentPercent()).isEqualTo(80);
                assertThat(result.reasons().getFirst().label()).isEqualTo("Sản phẩm bị lỗi");
                assertThat(result.reasons().getFirst().sharePercent()).isEqualTo(60);
                assertThat(result.productQuality().getFirst().requestsChangePercent()).isEqualTo(100);
                assertThat(result.productQuality().getFirst().approvalRate()).isEqualTo(75);
        }

        private static SalesSummaryProjection summary(
                        long totalOrders,
                        long completedOrders,
                        long cancelledOrders,
                        String completedSales,
                        long unitsSold,
                        long customers) {
                SalesSummaryProjection projection = mock(SalesSummaryProjection.class);
                when(projection.getTotalOrders()).thenReturn(totalOrders);
                // Marcus thêm: tỷ lệ hoàn tất dùng cùng tập đơn theo ngày tạo, không trộn với
                // ngày giao dịch.
                when(projection.getCohortCompletedOrders()).thenReturn(completedOrders);
                when(projection.getCompletedOrders()).thenReturn(completedOrders);
                when(projection.getCancelledOrders()).thenReturn(cancelledOrders);
                when(projection.getCompletedSales()).thenReturn(new BigDecimal(completedSales));
                when(projection.getUnitsSold()).thenReturn(unitsSold);
                when(projection.getOrderingCustomers()).thenReturn(customers);
                return projection;
        }

        private static CancellationReasonProjection cancellationReason(String reason, long count) {
                CancellationReasonProjection projection = mock(CancellationReasonProjection.class);
                when(projection.getReason()).thenReturn(reason);
                when(projection.getReasonCount()).thenReturn(count);
                return projection;
        }

        private static WarrantySummaryProjection warrantySummary(
                        long total, long pending, long processing, long approved, long rejected) {
                WarrantySummaryProjection projection = mock(WarrantySummaryProjection.class);
                when(projection.getTotalRequests()).thenReturn(total);
                when(projection.getPendingRequests()).thenReturn(pending);
                when(projection.getProcessingRequests()).thenReturn(processing);
                when(projection.getApprovedRequests()).thenReturn(approved);
                when(projection.getRejectedRequests()).thenReturn(rejected);
                return projection;
        }
}
