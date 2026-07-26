package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    @NotEmpty(message = "Giỏ hàng không được để trống")
    private List<@NotNull(message = "ID sản phẩm trong giỏ không được để trống") Integer> cartItemIds;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String recipientPhone;

    private String shippingAddress;

    private Integer toDistrictId;

    private String toWardCode;

    // Marcus thêm: DELIVERY hoặc STORE_PICKUP; mặc định DELIVERY để tương thích
    // client cũ.
    private String fulfillmentMethod = "DELIVERY";

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String voucherCode;
    private String provinceCity;

    // THÊM TRƯỜNG NÀY ĐỂ FIX LỖI
    private String note;
}
