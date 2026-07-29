package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.service.analytics.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional(readOnly = true)
class AnalyticsRepositoryIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    void executesAllAnalyticsQueriesAgainstSqlServer() {
        LocalDate toDate = LocalDate.now();
        LocalDate fromDate = toDate.minusDays(29);

        // Marcus thêm: chạy truy vấn native thật để phát hiện sớm lỗi cú pháp SQL
        // Server và lỗi mapping projection, không thay đổi dữ liệu.
        var overview = analyticsService.getOverview(fromDate, toDate);
        var trend = analyticsService.getSalesTrend(fromDate, toDate);
        var products = analyticsService.getProductTrends(fromDate, toDate, 10);

        assertThat(overview.period().numberOfDays()).isEqualTo(30);
        assertThat(overview.completedSales().currentValue()).isNotNull();
        assertThat(trend).hasSize(30);
        assertThat(products).hasSizeLessThanOrEqualTo(10);
    }
}
