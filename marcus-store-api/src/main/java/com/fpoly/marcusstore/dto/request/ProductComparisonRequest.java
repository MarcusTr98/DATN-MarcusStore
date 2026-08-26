package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProductComparisonRequest {

    @NotEmpty(message = "Cần ít nhất 2 sản phẩm để so sánh.")
    @Size(min = 2, max = 3, message = "Chỉ hỗ trợ so sánh từ 2 đến 3 sản phẩm.")
    private List<Integer> productIds;
}
