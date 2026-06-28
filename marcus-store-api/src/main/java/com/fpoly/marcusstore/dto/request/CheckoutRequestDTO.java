package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    @NotEmpty(message = "Giỏ hàng không được để trống")
    private List<Integer> cartItemIds;

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String shippingAddress;

    @NotNull(message = "ID Quận/Huyện không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Mã Phường/Xã không được để trống")
    private String toWardCode;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;

    private String voucherCode;

    // Tỉnh/thành phố giao hàng (để check voucher FREESHIP theo khu vực)
    private String provinceCity;
}