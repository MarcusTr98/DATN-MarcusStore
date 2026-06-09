package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequestDTO {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String recipientPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // VD: COD, VNPAY, QR

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán")
    private List<Integer> cartItemIds; // Danh sách ID của các món trong giỏ hàng muốn mua

    private Integer voucherId; // Truyền lên nếu có áp dụng mã giảm giá (Đạt sẽ xử lý sau)
}