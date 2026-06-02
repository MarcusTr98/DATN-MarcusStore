package com.fpoly.marcusstore.controller.admin;

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
        return ResponseEntity.ok(ApiResponse.success(valueService.getValuesByAttributeId(attributeId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttributeValue>> create(@RequestBody Map<String, Object> body) {
        try {
            Integer attributeId = Integer.parseInt(body.get("attributeId").toString());
            String valueString = body.get("valueString").toString();
            return ResponseEntity.ok(ApiResponse.success(valueService.createValue(attributeId, valueString)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AttributeValue>> update(@PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.success(valueService.updateValue(id, body.get("valueString"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
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