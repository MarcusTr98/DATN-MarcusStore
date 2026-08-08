package com.fpoly.marcusstore.dto.analytics;

import java.util.List;

/**
 * Marcus thêm: response riêng của tab Phân tích kinh doanh, chỉ đọc dữ liệu
 * tổng hợp từ Warranty_Returns và không can thiệp vòng đời bảo hành của Đạt.
 */
public record WarrantyAnalyticsResponse(
        AnalyticsPeriod period,
        AnalyticsMetric totalRequests,
        AnalyticsRateMetric resolutionRate,
        AnalyticsRateMetric approvalRate,
        long pendingRequests,
        long processingRequests,
        long approvedRequests,
        long rejectedRequests,
        List<WarrantyReasonMetric> reasons,
        List<ProductWarrantyQualityResponse> productQuality) {
}
