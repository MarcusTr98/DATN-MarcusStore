package com.fpoly.marcusstore.service.ai;

import com.fpoly.marcusstore.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiStoreKnowledgeService {

    private final SystemSettingService systemSettingService;

    // Marcus thêm: tri thức công khai lấy từ System Settings và khả năng thật của
    // Checkout; Gemini không được tự sáng tác chính sách cửa hàng.
    public String answer(AiAdvisorIntent intent) {
        Map<String, String> settings = systemSettingService.getPublicSettingsAsMap();
        String siteName = value(settings, "SITE_NAME", "Marcus Store");
        String address = value(settings, "ADDRESS", "118 Cát Bi, Hải An, Hải Phòng");
        return switch (intent) {
            case STORE_INFORMATION -> "Dạ, **" + siteName + "** ở địa chỉ " + address + ".";
            case STORE_PICKUP -> "Dạ được ạ. Tại Checkout, bạn chọn **Nhận tại cửa hàng** rồi đến "
                    + address + " sau khi đơn được xác nhận sẵn sàng nhận.";
            case PAYMENT_POLICY -> siteName + " hiện hỗ trợ **2 phương thức thanh toán**:\n"
                    + "- **COD:** thanh toán khi nhận hàng.\n"
                    + "- **VNPAY:** thanh toán trực tuyến trên website VNPAY.\n\n"
                    + "Bạn chọn phương thức phù hợp tại Checkout. Không gửi OTP, số thẻ hoặc thông tin ngân hàng qua chat nhé.";
            case PURCHASE_GUIDE -> "Nếu bạn đang ở trang sản phẩm vừa mở, hãy đặt hàng theo **4 bước**:\n"
                    + "- Chọn đúng màu sắc, dung lượng hoặc phiên bản SKU.\n"
                    + "- Nhấn **Thêm vào giỏ hàng** hoặc **Mua ngay** để mở Checkout.\n"
                    + "- Chọn **giao tận nơi** hoặc **nhận tại cửa hàng**.\n"
                    + "- Kiểm tra thông tin, chọn COD/VNPAY và xác nhận đặt hàng.\n\n"
                    + "Marcus AI chỉ hướng dẫn thao tác ở bước này, **không tự đổi sang sản phẩm khác**. "
                    + "Sau khi tạo đơn, bạn có thể theo dõi trong **Đơn hàng của tôi**.";
            default -> null;
        };
    }

    private String value(Map<String, String> settings, String key, String fallback) {
        if (settings == null)
            return fallback;
        String value = settings.get(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
