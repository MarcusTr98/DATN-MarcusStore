package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.entity.analytics.AnalyticsAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnalyticsActionRepository extends JpaRepository<AnalyticsAction, Long> {
    List<AnalyticsAction> findTop50ByOrderByUpdatedAtDesc();

    boolean existsByTitleAndStatusIn(String title, List<String> statuses);
}
