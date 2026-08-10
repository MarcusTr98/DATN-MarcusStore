package com.fpoly.marcusstore.service.ai;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AiAdvisorIntentRouter {

    public AiAdvisorIntent detect(String rawMessage, boolean hasFocusedProduct) {
        String message = rawMessage == null ? "" : rawMessage.toLowerCase(Locale.forLanguageTag("vi-VN"));
        if (message.contains("địa chỉ") || (message.contains("ở đâu")
                && (message.contains("marcus") || message.contains("cửa hàng")))) {
            return AiAdvisorIntent.STORE_INFORMATION;
        }
        if (message.matches(".*(nhận hàng tại|nhận tại cửa hàng|đến cửa hàng nhận).*")) {
            return AiAdvisorIntent.STORE_PICKUP;
        }
        if (message.matches(".*(thanh toán thế nào|thanh toán như thế nào|hình thức thanh toán|"
                + "phương thức thanh toán|thanh toán bằng gì|có cod|có vnpay).*")) {
            return AiAdvisorIntent.PAYMENT_POLICY;
        }
        if (message.matches(".*(mua thế nào|mua như thế nào|mua hàng thế nào|mua hàng như thế nào|"
                + "cách mua|cách đặt hàng|đặt thế nào|đặt như thế nào|đặt hàng thế nào|"
                + "đặt hàng như thế nào|làm sao để mua|làm sao đặt|quy trình mua).*")) {
            return AiAdvisorIntent.PURCHASE_GUIDE;
        }
        if (message.matches(".*(hãng gì|của hãng nào|thương hiệu nào).*")) {
            return AiAdvisorIntent.BRAND_KNOWLEDGE;
        }
        boolean price = message.matches(".*(giá|bao nhiêu tiền|bao nhiêu|tầm giá|khoảng giá).*");
        boolean product = message.matches(".*(iphone|ipad|airpods|galaxy|redmi|samsung|xiaomi|oppo|"
                + "vivo|realme|honor|nokia|điện thoại|smartphone|phụ kiện|sản phẩm|model|mẫu|máy|"
                + "con này|cái này).*");
        if (price && product)
            return AiAdvisorIntent.PRICE_LOOKUP;
        if (hasFocusedProduct && message.matches(".*(con này|máy này|cái này|sản phẩm này|vừa xem).*")) {
            return AiAdvisorIntent.PRODUCT_FOLLOW_UP;
        }
        return AiAdvisorIntent.PRODUCT_ADVICE;
    }
}
