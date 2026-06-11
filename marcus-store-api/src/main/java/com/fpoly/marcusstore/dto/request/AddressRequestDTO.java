package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AddressRequestDTO {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 số và bắt đầu bằng số 0")
    private String phoneNumber;

    @NotBlank(message = "Vui lòng chọn Tỉnh/Thành phố")
    private String provinceName;

    @NotBlank(message = "Vui lòng chọn Quận/Huyện")
    private String districtName;

    @NotBlank(message = "Vui lòng chọn Phường/Xã")
    private String wardName;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detailAddress;

    private String note;

    private Boolean isDefault;

    // Tọa độ cắm ghim trên Map (Frontend sẽ gửi lên)
    private BigDecimal latitude;
    private BigDecimal longitude;
}