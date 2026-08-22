package com.fpoly.marcusstore.utils;

import java.util.Locale;

/**
 * Marcus thêm: registry duy nhất quyết định mức độ, icon và deep link của
 * chuông.
 * Frontend chỉ render metadata backend trả về nên Admin/Client không tự đoán
 * loại.
 */
public final class NotificationRegistry {

    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ACTION_REQUIRED = "ACTION_REQUIRED";

    private NotificationRegistry() {
    }

    public static Metadata forAdmin(String rawType, String referenceId) {
        String type = normalize(rawType);
        if (type.startsWith("WARRANTY_")) {
            return new Metadata(ACTION_REQUIRED, "fa-solid fa-shield-halved",
                    referenceId == null ? "/admin/warranty" : "/admin/warranty/" + referenceId);
        }
        if ("CONTACT".equals(type)) {
            return new Metadata(ACTION_REQUIRED, "fa-solid fa-envelope-open-text", "/admin/contact-management");
        }
        if (type.contains("REFUND")) {
            return new Metadata(ACTION_REQUIRED, "fa-solid fa-money-bill-transfer",
                    orderAdminLink(referenceId));
        }
        if (type.contains("FAILED") || type.contains("CANCELLED") || type.startsWith("GHN_")) {
            return new Metadata(WARNING, "fa-solid fa-triangle-exclamation", orderAdminLink(referenceId));
        }
        if (type.startsWith("ORDER") || type.startsWith("PAYMENT")) {
            return new Metadata(INFO, "fa-solid fa-box", orderAdminLink(referenceId));
        }
        return new Metadata(INFO, "fa-solid fa-bell", null);
    }

    public static Metadata forUser(String rawType, String referenceId) {
        String type = normalize(rawType);
        if (type.startsWith("STAFF_ORDER_")) {
            return new Metadata(ACTION_REQUIRED, "fas fa-clipboard-check", orderAdminLink(referenceId));
        }
        String orderLink = referenceId == null ? null : "/profile/orders/" + referenceId;
        if (type.startsWith("WARRANTY_")) {
            return new Metadata(INFO, "fas fa-shield-halved", orderLink);
        }
        if (type.startsWith("REFUND_")) {
            String category = type.endsWith("FAILED") ? WARNING : INFO;
            return new Metadata(category, "fas fa-money-bill-transfer", orderLink);
        }
        if (type.endsWith("CANCELLED") || type.endsWith("FAILED")) {
            return new Metadata(WARNING, "fas fa-triangle-exclamation", orderLink);
        }
        return new Metadata(INFO, iconForOrder(type), orderLink);
    }

    public static String eventKey(String audience, String type, String referenceId, String discriminator) {
        return String.join(":",
                normalize(audience), normalize(type), safe(referenceId), safe(discriminator));
    }

    private static String orderAdminLink(String referenceId) {
        return referenceId == null ? "/admin/order" : "/admin/order/" + referenceId;
    }

    private static String iconForOrder(String type) {
        return switch (type) {
            case "ORDER_PENDING" -> "fas fa-receipt";
            case "ORDER_CONFIRMED", "ORDER_DELIVERED", "ORDER_COMPLETED", "PAYMENT_SUCCESS" ->
                "fas fa-circle-check";
            case "ORDER_PROCESSING" -> "fas fa-box-open";
            case "ORDER_READY_FOR_PICKUP" -> "fas fa-store";
            case "ORDER_PACKED" -> "fas fa-box";
            case "ORDER_SHIPPING" -> "fas fa-truck-fast";
            default -> "far fa-bell";
        };
    }

    private static String normalize(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank())
            return "NONE";
        return value.trim().replace(':', '_').substring(0, Math.min(value.trim().length(), 80));
    }

    public record Metadata(String category, String icon, String deepLink) {
    }
}
