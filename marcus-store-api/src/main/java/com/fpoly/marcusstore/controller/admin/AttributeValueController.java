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

@RestController
@RequestMapping("/api/admin/attribute-values")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AttributeValueController {

    @Autowired
    private AttributeValueService valueService;

    @GetMapping("/attribute/{attributeId}")
    public ResponseEntity<ApiResponse<List<AttributeValue>>> getValuesByAttribute(@PathVariable Integer attributeId) {
        // Kiểm tra xem service này có trả về đúng dữ liệu không
        List<AttributeValue> list = valueService.getValuesByAttributeId(attributeId);
        System.out.println("DEBUG: Số lượng giá trị tìm được: " + list.size());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // Marcus sửa vì đã thêm dto
    @PostMapping
    public ApiResponse<AttributeValue> create(@RequestBody AttributeValueRequest req) {
        return ApiResponse.success(valueService.createValue(req.getAttributeId(), req.getValueString()));
    }

    @PutMapping("/{id}")
    public ApiResponse<AttributeValue> update(@PathVariable Integer id, @RequestBody AttributeValueRequest req) {
        return ApiResponse.success(valueService.updateValue(id, req.getValueString()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        try {
            valueService.deleteValue(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa giá trị thuộc tính thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}