package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.service.SystemSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SystemSettingController {

    @Autowired
    private SystemSettingService service;

    @GetMapping("/public/settings")
    public Map<String, String> getPublicSettings() {
        return service.getAllSettingsAsMap();
    }

    @PutMapping("/admin/settings/bulk-update")
    public Map<String, Object> updateSettings(@RequestBody Map<String, String> payload) {
        service.updateSettings(payload);
        return Map.of(
                "status", 200,
                "message", "Cập nhật cấu hình hệ thống thành công!");
    }
}