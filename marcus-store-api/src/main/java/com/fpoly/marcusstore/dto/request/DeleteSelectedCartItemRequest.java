package com.fpoly.marcusstore.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Getter
@Setter
public class DeleteSelectedCartItemRequest {
    // Marcus thêm validation bảo vệ request đầu vào; không thay đổi nghiệp vụ Cart
    // của thành viên.
    @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
    @Size(max = 100, message = "Chỉ được xóa tối đa 100 sản phẩm mỗi lần")
    private List<@Positive(message = "SKU không hợp lệ") Integer> skuIds;
}
