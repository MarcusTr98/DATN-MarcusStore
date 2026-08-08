package com.fpoly.marcusstore.dto.analytics;

/**
 * Marcus thêm cho module Analytics: chỉ biểu diễn lý do bảo hành dạng enum đã
 * tổng hợp. Không thay đổi DTO hay nghiệp vụ bảo hành do Đạt phụ trách.
 */
public record WarrantyReasonMetric(
        String reason,
        String label,
        long count,
        double sharePercent) {
}
