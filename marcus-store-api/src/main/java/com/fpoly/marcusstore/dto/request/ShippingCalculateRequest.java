package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ShippingCalculateRequest {

    @NotNull(message = "ID Quận/Huyện không được để trống")
    private Integer toDistrictId;

    @NotBlank(message = "Mã Phường/Xã không được để trống")
    private String toWardCode;

    // Marcus sửa: client chỉ gửi các dòng Cart được chọn. Khối lượng và giá phải
    // đọc lại từ database để phí xem trước khớp với lúc tạo đơn.
    @NotEmpty(message = "Danh sách sản phẩm tính phí không được để trống")
    @Size(max = 100, message = "Chỉ được tính phí tối đa 100 dòng sản phẩm")
    private List<@NotNull @Positive Integer> cartItemIds;
}
