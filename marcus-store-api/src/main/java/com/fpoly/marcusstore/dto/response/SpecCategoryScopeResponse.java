package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Marcus thêm cho luồng thông số của Đức: FE cần biết rõ danh mục cha/con
// để Admin chủ động chọn phạm vi dùng chung, không tự đẩy thuộc tính lên cha.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecCategoryScopeResponse {
    private Integer categoryId;
    private String categoryName;
    private boolean productCategory;
}
