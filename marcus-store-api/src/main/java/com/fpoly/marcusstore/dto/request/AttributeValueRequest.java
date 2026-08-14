package com.fpoly.marcusstore.dto.request;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class AttributeValueRequest {
    @NotNull(message = "Thuộc tính không được để trống")
    @Positive(message = "ID thuộc tính không hợp lệ")
    private Integer attributeId;
    @NotBlank(message = "Giá trị thuộc tính không được để trống")
    @Size(max = 100, message = "Giá trị thuộc tính không được vượt quá 100 ký tự")
    private String valueString;
    @Size(max = 50, message = "Dữ liệu bổ sung không được vượt quá 50 ký tự")
    private String valueMeta;
}
