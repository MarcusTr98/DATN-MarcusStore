package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequestDTO {
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String provinceName;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String districtName;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardName;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detailAddress;

    private String note;

    private Boolean isDefault = false;
}