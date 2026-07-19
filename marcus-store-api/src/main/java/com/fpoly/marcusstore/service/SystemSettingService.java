package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.cms.SystemSetting;
import com.fpoly.marcusstore.repository.cms.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsAsMap() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    @Transactional
    public void updateSettings(Map<String, String> payload) {
        List<SystemSetting> existingSettings = repository.findAllById(payload.keySet());

        Map<String, SystemSetting> existingMap = existingSettings.stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));

        List<SystemSetting> toSave = new ArrayList<>();

        for (Map.Entry<String, String> entry : payload.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // Bỏ qua nếu giá trị null
            if (value == null)
                continue;

            SystemSetting setting = existingMap.get(key);

            if (setting != null) {
                // Đã tồn tại -> Chỉ cập nhật giá trị
                setting.setSettingValue(value);
            } else {
                // Chưa tồn tại -> Khởi tạo Entity và đổ đầy các cột NOT NULL
                setting = new SystemSetting();
                setting.setSettingKey(key);
                setting.setSettingValue(value);

                setting.setSettingGroup(determineSettingGroup(key));
                setting.setDescription("Cấu hình tự động khởi tạo từ hệ thống quản trị");
            }
            toSave.add(setting);
        }

        repository.saveAll(toSave);
    }

    private String determineSettingGroup(String key) {
        String upperKey = key.toUpperCase();
        if (upperKey.contains("HERO") || upperKey.contains("PROMO")) {
            return "CONTENT";
        }
        if (upperKey.contains("URL") || upperKey.contains("FACEBOOK") || upperKey.contains("TIKTOK")
                || upperKey.contains("YOUTUBE") || upperKey.contains("INSTAGRAM")) {
            return "SOCIAL";
        }
        if (upperKey.contains("HOTLINE") || upperKey.contains("EMAIL") || upperKey.contains("WORKING_HOURS")
                || upperKey.contains("ADDRESS")) {
            return "CONTACT";
        }
        return "GENERAL"; // Nhóm mặc định cho STORE_LOCATION và các key khác
    }
}