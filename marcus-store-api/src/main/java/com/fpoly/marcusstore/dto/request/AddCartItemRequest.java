package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
public class AddCartItemRequest {
    // Marcus thêm validation tại điểm giao Cart -> Checkout: chặn request giả mạo
    // SKU/số lượng trước khi dữ liệu đi vào luồng Checkout do Marcus phụ trách.
    @NotNull(message = "SKU không được để trống")
    @Positive(message = "SKU không hợp lệ")
    private Integer skuId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng mua phải lớn hơn 0")
    @Max(value = 100, message = "Mỗi lần chỉ được thêm tối đa 100 sản phẩm")
    private Integer quantity;

    // ID của FlashSaleSlot - nếu user mua từ trang Flash Sale
    // Nếu NULL hoặc không có → coi như sản phẩm bình thường
    // Marcus sửa bảo mật Checkout: frontend chỉ gửi ID Flash Sale để backend đối
    // chiếu; không được gửi giá và yêu cầu backend tin theo.
    @Positive(message = "Flash Sale không hợp lệ")
    private Long flashSaleSlotId;
}
