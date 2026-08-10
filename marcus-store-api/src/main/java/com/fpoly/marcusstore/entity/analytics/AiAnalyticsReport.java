package com.fpoly.marcusstore.entity.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "AI_Analytics_Reports")
@Getter
@Setter
public class AiAnalyticsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    // Marcus thêm: lưu đúng JSON đã kiểm duyệt để lần sau chỉ đọc DB, không gọi AI.
    @Column(name = "report_json", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String reportJson;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    // Marcus thêm: nhận diện dữ liệu nguồn đã đổi để không dùng nhầm báo cáo cache cũ.
    @Column(name = "data_fingerprint", length = 64)
    private String dataFingerprint;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    void initializeGeneratedAt() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }
}
