package com.fpoly.marcusstore.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImportRequest {
    @NotNull(message = "Vui lòng chọn SKU")
    private Integer skuId;

    @NotNull
    @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
    private Integer importQuantity;

    private String note;

    private List<String> imeis;
}