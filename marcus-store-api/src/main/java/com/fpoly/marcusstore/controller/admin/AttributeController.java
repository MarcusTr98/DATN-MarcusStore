package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.request.AttributeRequest;
import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.service.AttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/attributes")
@PreAuthorize("hasAuthority('ATTRIBUTE_VIEW')")
@Validated
public class AttributeController {

    @Autowired
    private AttributeService attributeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Attribute>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAllAttributes()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ATTRIBUTE_CREATE')")
    public ResponseEntity<ApiResponse<Attribute>> create(@Valid @RequestBody AttributeRequest request) {
        // Marcus sửa: dùng DTO thay Map để backend bắt tên rỗng/quá dài/sai định dạng.
        return ResponseEntity.ok(ApiResponse.success(attributeService.createAttribute(request.getName().trim())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTRIBUTE_UPDATE')")
    public ResponseEntity<ApiResponse<Attribute>> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody AttributeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(attributeService.updateAttribute(id, request.getName().trim())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ATTRIBUTE_DELETE')")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable @Positive Integer id) {
        try {
            attributeService.deleteAttribute(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}
