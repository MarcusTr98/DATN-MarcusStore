package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Marcus thêm: dữ liệu tổng hợp cho màn quản lý bộ thông số theo danh mục.
// Không tạo bảng "bộ thông số" mới vì Category + Spec_Attributes đã là nguồn chuẩn.
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecCategoryOverviewResponse {
    private Integer categoryId;
    private String categoryName;
    private Integer parentId;
    private String parentName;
    private Boolean status;
    private long directAttributeCount;
    private long inheritedAttributeCount;
    private long effectiveAttributeCount;
    private long productCount;
}
