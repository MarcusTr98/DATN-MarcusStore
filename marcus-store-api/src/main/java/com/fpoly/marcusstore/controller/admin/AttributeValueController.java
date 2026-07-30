package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AttributeValueRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.service.AttributeValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/attribute-values")
@PreAuthorize("hasAuthority('ATTRIBUTE_VALUE_VIEW')")
@Validated
public class AttributeValueController {

    @Autowired
    private AttributeValueService valueService;

    @GetMapping("/attribute/{attributeId}")
    public ResponseEntity<ApiResponse<List<AttributeValue>>> getValuesByAttribute(
            @PathVariable @Positive Integer attributeId) {
        List<AttributeValue> list = valueService.getValuesByAttributeId(attributeId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // Marcus sửa vì đã thêm dto, nâng cấp tiếp theo demo
    @PostMapping
    @PreAuthorize("hasAuthority('ATTRIBUTE_VALUE_CREATE')")
    public ApiResponse<AttributeValue> create(@Valid @RequestBody AttributeValueRequest req) {
        return ApiResponse
                .success(valueService.createValue(req.getAttributeId(), req.getValueString(), req.getValueMeta()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTRIBUTE_VALUE_UPDATE')")
    public ApiResponse<AttributeValue> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody AttributeValueRequest req) {
        return ApiResponse.success(valueService.updateValue(id, req.getValueString(), req.getValueMeta()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTRIBUTE_VALUE_DELETE')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable @Positive Integer id) {
        try {
            valueService.deleteValue(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa giá trị thuộc tính thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
