package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
public class UpdateCartItemRequest {
    // Marcus thêm validation tại biên Cart -> Checkout để giỏ không lưu số lượng
    // sai, tránh làm sai tiền và tồn kho khi Checkout.
    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    @Max(value = 100, message = "Số lượng tối đa là 100")
    private Integer quantity;
}
