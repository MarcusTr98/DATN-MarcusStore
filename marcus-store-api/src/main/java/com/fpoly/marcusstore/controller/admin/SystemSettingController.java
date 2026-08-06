package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.service.SystemSettingService;
import com.fpoly.marcusstore.service.CloudinaryService;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.request.BulkUpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class SystemSettingController {

    @Autowired
    private SystemSettingService service;
    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/public/settings")
    public Map<String, String> getPublicSettings() {
        return service.getPublicSettingsAsMap();
    }

    @GetMapping("/admin/settings")
    // Marcus thêm: tách quyền xem và cập nhật cấu hình hệ thống.
    @PreAuthorize("hasAuthority('SYSTEM_VIEW')")
    public Map<String, String> getAdminSettings() {
        return service.getAllSettingsAsMap();
    }

    @PutMapping("/admin/settings/bulk-update")
    @PreAuthorize("hasAuthority('SYSTEM_UPDATE')")
    public Map<String, Object> updateSettings(@Valid @RequestBody BulkUpdateSettingsRequest request) {
        // Marcus sửa: request có DTO riêng; không nhận Map tùy ý trực tiếp từ client.
        service.updateSettings(request.getSettings());
        return Map.of(
                "status", 200,
                "message", "Cập nhật cấu hình hệ thống thành công!");
    }

    @PostMapping(value = "/admin/settings/upload-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SYSTEM_UPDATE')")
    public ApiResponse<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file)
            throws Exception {
        // Marcus thêm: validate tại backend, không tin thuộc tính accept của input.
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh Logo.");
        }
        if (file.getSize() > 2L * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh Logo không được vượt quá 2 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new IllegalArgumentException("Logo chỉ hỗ trợ JPG, PNG hoặc WEBP.");
        }
        String imageUrl = cloudinaryService.uploadImage(file);
        return ApiResponse.success(Map.of("imageUrl", imageUrl));
    }
}
