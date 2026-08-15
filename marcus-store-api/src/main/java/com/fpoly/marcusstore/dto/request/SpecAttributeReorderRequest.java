package com.fpoly.marcusstore.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Marcus thêm: đổi thứ tự cả bộ trong một transaction, tránh cập nhật nửa chừng.
@Getter
@Setter
public class SpecAttributeReorderRequest {
    @NotNull
    @Positive
    private Integer categoryId;

    @NotEmpty
    private List<@NotNull @Positive Integer> attributeIds;
}
