package com.fpoly.marcusstore.utils;

import java.util.Map;

/**
 * Marcus thêm: mã lý do hủy ổn định để Analytics không phải đoán từ câu chữ.
 */
public final class CancellationReasonCatalog {

    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("CUSTOMER_WRONG_ITEM", "Đặt nhầm sản phẩm hoặc số lượng"),
            Map.entry("CUSTOMER_CHANGE_ADDRESS", "Muốn thay đổi địa chỉ nhận hàng"),
            Map.entry("CUSTOMER_BETTER_OPTION", "Tìm được sản phẩm hoặc giá phù hợp hơn"),
            Map.entry("CUSTOMER_DELIVERY_TIME", "Thời gian giao hàng không phù hợp"),
            Map.entry("CUSTOMER_NO_DEMAND", "Không còn nhu cầu mua"),
            Map.entry("CUSTOMER_OTHER", "Lý do khác từ khách hàng"),
            Map.entry("ADMIN_CUSTOMER_REQUEST", "Khách hàng yêu cầu hủy"),
            Map.entry("ADMIN_CANNOT_CONTACT", "Không liên hệ được với khách hàng"),
            Map.entry("ADMIN_OUT_OF_STOCK", "Sản phẩm hết hàng hoặc lỗi tồn kho"),
            Map.entry("ADMIN_INVALID_ADDRESS", "Thông tin nhận hàng không hợp lệ"),
            Map.entry("ADMIN_SUSPICIOUS_ORDER", "Phát hiện đơn hàng bất thường"),
            Map.entry("ADMIN_OTHER", "Lý do khác từ Admin"),
            Map.entry("SYSTEM_VNPAY_EXPIRED", "Quá hạn thanh toán VNPAY"),
            Map.entry("SYSTEM_VNPAY_FAILED", "Thanh toán VNPAY không thành công"),
            Map.entry("SYSTEM_COD_CONFIRMATION_EXPIRED", "Đơn COD quá hạn chờ xác nhận"),
            Map.entry("SYSTEM_OTHER", "Hệ thống tự hủy"),
            Map.entry("GHN_CANCELLED", "Đơn vị vận chuyển hủy vận đơn"));

    private CancellationReasonCatalog() {
    }

    public static String normalizeCode(String code, String actor) {
        if (code != null && LABELS.containsKey(code.trim().toUpperCase())) {
            return code.trim().toUpperCase();
        }
        return switch (actor == null ? "SYSTEM" : actor.trim().toUpperCase()) {
            case "CUSTOMER" -> "CUSTOMER_OTHER";
            case "ADMIN" -> "ADMIN_OTHER";
            case "GHN" -> "GHN_CANCELLED";
            default -> "SYSTEM_OTHER";
        };
    }

    public static String label(String code) {
        return LABELS.getOrDefault(code, "Lý do khác");
    }
}
