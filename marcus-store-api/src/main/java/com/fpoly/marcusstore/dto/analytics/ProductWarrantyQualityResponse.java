package com.fpoly.marcusstore.dto.analytics;

/**
 * Marcus thêm cho module Analytics: số liệu bảo hành tổng hợp theo sản phẩm.
 * DTO không chứa khách hàng, mô tả, ghi chú Admin hoặc tệp bảo hành của Đạt.
 */
public record ProductWarrantyQualityResponse(
                Integer productId,
                String productName,
                String brand,
                long currentRequests,
                long previousRequests,
                Double requestsChangePercent,
                long approvedRequests,
                long rejectedRequests,
                double approvalRate) {
}
