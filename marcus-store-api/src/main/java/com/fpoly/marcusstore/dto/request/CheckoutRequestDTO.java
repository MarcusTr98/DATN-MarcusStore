package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CheckoutRequestDTO {
    // Marcus thêm: UUID ổn định trong suốt một lần Checkout để backend nhận ra
    // request retry và không tạo hai đơn.
    @NotBlank(message = "Mã yêu cầu Checkout không được để trống")
    @Size(max = 64, message = "Mã yêu cầu Checkout không hợp lệ")
    @Pattern(regexp = "^[A-Za-z0-9_-]{16,64}$", message = "Mã yêu cầu Checkout sai định dạng")
    private String checkoutRequestId;

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Size(max = 100, message = "Mỗi đơn hàng chỉ được có tối đa 100 dòng sản phẩm")
    private List<@NotNull(message = "ID sản phẩm trong giỏ không được để trống") @Positive(message = "ID sản phẩm trong giỏ không hợp lệ") Integer> cartItemIds;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận không được vượt quá 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
    private String recipientPhone;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String shippingAddress;

    @Positive(message = "Quận/Huyện không hợp lệ")
    private Integer toDistrictId;

    @Size(max = 20, message = "Mã Phường/Xã không hợp lệ")
    private String toWardCode;

    // Marcus thêm: DELIVERY hoặc STORE_PICKUP; mặc định DELIVERY để tương thích
    // client cũ.
    @Pattern(regexp = "(?i)DELIVERY|STORE_PICKUP", message = "Phương thức nhận hàng không hợp lệ")
    private String fulfillmentMethod = "DELIVERY";

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    @Pattern(regexp = "(?i)COD|VNPAY", message = "Phương thức thanh toán chỉ hỗ trợ COD hoặc VNPAY")
    private String paymentMethod;

    @Size(max = 50, message = "Mã voucher không hợp lệ")
    private String voucherCode;

    @Size(max = 100, message = "Tỉnh/Thành phố không hợp lệ")
    private String provinceCity;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note;
}
