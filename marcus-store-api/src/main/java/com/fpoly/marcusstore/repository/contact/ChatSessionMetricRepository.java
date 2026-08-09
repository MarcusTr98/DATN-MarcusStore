package com.fpoly.marcusstore.repository.contact;

import com.fpoly.marcusstore.entity.interaction.ChatSessionMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionMetricRepository extends JpaRepository<ChatSessionMetric, String> {
}
