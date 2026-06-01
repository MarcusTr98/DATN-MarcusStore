package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SkuGenerationRequest {

    @NotNull(message = "ID Sản phẩm không được để trống")
    private Integer productId;

    @NotEmpty(message = "Mã sản phẩm (Prefix) không được để trống")
    private String productCode;

    // Vdu gửi: [ [1, 2], [3, 4, 5] ] (ID của Đỏ, Xanh và 128GB, 256GB, 512GB)
    @NotEmpty(message = "Danh sách thuộc tính không được để trống")
    private List<List<Integer>> attributeValueIds;

    // Lưu String value để nối chuỗi SKU Code (VD: ["RED", "BLUE"],
    // ["128GB","256GB"])
    @NotEmpty(message = "Danh sách mã thuộc tính không được để trống")
    private List<List<String>> attributeValueCodes;
}