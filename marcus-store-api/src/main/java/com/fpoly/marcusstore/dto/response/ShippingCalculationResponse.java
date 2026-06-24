package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ShippingCalculationResponse {
    private BigDecimal standardShippingFee; // Phí thực tế lấy từ GHN
    private BigDecimal discountedShippingFee; // Phí khách phải trả sau khi tính toán
    private Boolean isFreeship;

    private Boolean isAllowedToOrder; // False nếu đơn dưới 200k
    private String blockMessage; // Đơn hàng tối thiểu là 200.000đ

    private BigDecimal amountUntilFreeship; // Số tiền cần mua thêm
    private String suggestionMessage; // Dòng chữ upsell hiển thị trên UI
}