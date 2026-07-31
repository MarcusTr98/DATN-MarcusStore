package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.service.SystemSettingService;
import com.fpoly.marcusstore.dto.request.BulkUpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemSettingController {

    @Autowired
    private SystemSettingService service;

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
}
