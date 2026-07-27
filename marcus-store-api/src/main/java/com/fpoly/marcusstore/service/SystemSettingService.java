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
import java.util.Set;

@Service
public class SystemSettingService {

    private static final Set<String> PUBLIC_SETTING_KEYS = Set.of(
            "HOTLINE", "EMAIL", "ADDRESS", "WORKING_HOURS", "PROMO_TEXT",
            "FACEBOOK_URL", "TIKTOK_URL", "INSTAGRAM_URL", "YOUTUBE_URL",
            "STORE_LOCATION", "HOME_HERO_BADGE", "HOME_HERO_TITLE",
            "HOME_HERO_TITLE_ACCENT", "HOME_HERO_LEAD", "HOME_HERO_SLIDES");

    @Autowired
    private SystemSettingRepository repository;

    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsAsMap() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    // Marcus sửa: client công khai chỉ nhận các cấu hình hiển thị đã allowlist,
    // không lộ prompt/chính sách nội bộ trong System_Settings.
    @Transactional(readOnly = true)
    public Map<String, String> getPublicSettingsAsMap() {
        return repository.findAllById(PUBLIC_SETTING_KEYS).stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    @Transactional(readOnly = true)
    public String getInternalSetting(String key, String fallback) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .filter(value -> !value.isBlank())
                .orElse(fallback);
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

            if ("AI_ADVISOR_POLICY".equalsIgnoreCase(key)) {
                value = validateAiPolicy(value);
            }

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

    private String validateAiPolicy(String value) {
        String policy = value.trim();
        if (policy.length() > 1_000) {
            throw new IllegalArgumentException("Chính sách Marcus AI không được vượt quá 1.000 ký tự.");
        }
        if (policy.matches("(?is).*(bỏ qua|phớt lờ).*(quy tắc|chỉ dẫn|bảo mật|system|prompt).*")
                || policy.matches("(?is).*(sql|database|api[ -]?key|mật khẩu|password|delete|update|insert).*")) {
            throw new IllegalArgumentException(
                    "Chính sách Marcus AI chứa chỉ dẫn có thể làm suy yếu quy tắc bảo mật.");
        }
        return policy;
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
        if (upperKey.startsWith("AI_")) {
            return "AI";
        }
        return "GENERAL"; // Nhóm mặc định cho STORE_LOCATION và các key khác
    }
}
