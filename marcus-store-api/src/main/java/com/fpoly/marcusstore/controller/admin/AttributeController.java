package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.service.AttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/attributes")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AttributeController {

    @Autowired
    private AttributeService attributeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Attribute>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAllAttributes()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Attribute>> create(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.success(attributeService.createAttribute(body.get("name"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Attribute>> update(@PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponse.success(attributeService.updateAttribute(id, body.get("name"))));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id) {
        try {
            attributeService.deleteAttribute(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
}