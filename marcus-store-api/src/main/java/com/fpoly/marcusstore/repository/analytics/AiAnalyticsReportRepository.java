package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.entity.analytics.AiAnalyticsReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface AiAnalyticsReportRepository extends JpaRepository<AiAnalyticsReport, Long> {

    Optional<AiAnalyticsReport> findFirstByFromDateAndToDateOrderByGeneratedAtDesc(
            LocalDate fromDate,
            LocalDate toDate);
}
