package com.fpoly.marcusstore.service.ai;

// Marcus thêm: phân loại ý định trước khi quyết định đọc catalog hay trả lời
// nghiệp vụ, tránh một câu hỏi thanh toán lại bị ép thành tư vấn sản phẩm.
public enum AiAdvisorIntent {
    PRODUCT_ADVICE,
    PRODUCT_FOLLOW_UP,
    PRICE_LOOKUP,
    BRAND_KNOWLEDGE,
    PURCHASE_GUIDE,
    PAYMENT_POLICY,
    STORE_PICKUP,
    STORE_INFORMATION
}
