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
import java.net.URI;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.dto.response.SystemSettingsAdminResponse;
import java.time.LocalDateTime;

@Service
public class SystemSettingService {

    private static final Map<String, String> DEFAULT_SETTINGS = Map.ofEntries(
            Map.entry("SITE_NAME", "Marcus Store"), Map.entry("SITE_LOGO_URL", ""),
            Map.entry("HOTLINE", "0907640098"), Map.entry("EMAIL", "support@marcusstore.com"),
            Map.entry("ADDRESS", "118 Cát Bi, Hải An, Hải Phòng"),
            Map.entry("WORKING_HOURS", "08:00 - 21:00 (Thứ Hai - Chủ Nhật)"),
            Map.entry("PROMO_TEXT", "Hàng chính hãng - Giá minh bạch - Hỗ trợ tận tâm"),
            Map.entry("FACEBOOK_URL", ""), Map.entry("TIKTOK_URL", ""),
            Map.entry("INSTAGRAM_URL", ""), Map.entry("YOUTUBE_URL", ""),
            Map.entry("STORE_LOCATION",
                    "{\"lat\":20.82716,\"lng\":106.70466,\"name\":\"Marcus Store\",\"address\":\"118 Cát Bi, Hải An, Hải Phòng\"}"),
            Map.entry("HOME_HERO_BADGE", "Công nghệ chính hãng tại Hải Phòng"),
            Map.entry("HOME_HERO_TITLE", "Chọn đúng thiết bị."),
            Map.entry("HOME_HERO_TITLE_ACCENT", "Tận hưởng đúng nhu cầu."),
            Map.entry("HOME_HERO_LEAD",
                    "Khám phá điện thoại và phụ kiện chính hãng, giá minh bạch cùng dịch vụ hỗ trợ tận tâm tại Marcus Store."),
            Map.entry("HOME_HERO_SLIDES", """
                    [{"kicker":"NỔI BẬT","name":"Điện thoại chính hãng","price":"Giá minh bạch","tag":"Bảo hành uy tín"},{"kicker":"LỰA CHỌN THÔNG MINH","name":"Phụ kiện phù hợp","price":"Đa dạng lựa chọn","tag":"Tư vấn tận tâm"},{"kicker":"MUA SẮM THUẬN TIỆN","name":"Nhận hàng tại cửa hàng","price":"Miễn phí nhận hàng","tag":"118 Cát Bi, Hải Phòng"}]
                    """),
            Map.entry("AI_ADVISOR_POLICY",
                    "Trả lời thân thiện, ngắn gọn, lịch sự và dễ hiểu với khách hàng trẻ."));

    private static final Set<String> PUBLIC_SETTING_KEYS = Set.of(
            "SITE_NAME", "SITE_LOGO_URL",
            "HOTLINE", "EMAIL", "ADDRESS", "WORKING_HOURS", "PROMO_TEXT",
            "FACEBOOK_URL", "TIKTOK_URL", "INSTAGRAM_URL", "YOUTUBE_URL",
            "STORE_LOCATION", "HOME_HERO_BADGE", "HOME_HERO_TITLE",
            "HOME_HERO_TITLE_ACCENT", "HOME_HERO_LEAD", "HOME_HERO_SLIDES");
    private static final Set<String> ADMIN_EDITABLE_KEYS = Set.of(
            "SITE_NAME", "SITE_LOGO_URL",
            "HOTLINE", "EMAIL", "ADDRESS", "WORKING_HOURS", "PROMO_TEXT",
            "FACEBOOK_URL", "TIKTOK_URL", "INSTAGRAM_URL", "YOUTUBE_URL",
            "STORE_LOCATION", "HOME_HERO_BADGE", "HOME_HERO_TITLE",
            "HOME_HERO_TITLE_ACCENT", "HOME_HERO_LEAD", "HOME_HERO_SLIDES",
            "AI_ADVISOR_POLICY");
    private static final Set<String> URL_KEYS = Set.of(
            "SITE_LOGO_URL", "FACEBOOK_URL", "TIKTOK_URL", "INSTAGRAM_URL", "YOUTUBE_URL");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private SystemSettingRepository repository;
    @Autowired
    private ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Map<String, String> getAllSettingsAsMap() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    @Transactional(readOnly = true)
    public SystemSettingsAdminResponse getAdminSettings() {
        List<SystemSetting> all = repository.findAll();
        SystemSetting latest = all.stream()
                .filter(item -> item.getUpdatedAt() != null)
                .max(java.util.Comparator.comparing(SystemSetting::getUpdatedAt)).orElse(null);
        return new SystemSettingsAdminResponse(
                all.stream().collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue)),
                latest == null ? null : latest.getUpdatedBy(),
                latest == null ? null : latest.getUpdatedAt());
    }

    // Marcus sửa: client công khai chỉ nhận các cấu hình hiển thị đã allowlist,
    // không lộ prompt/chính sách nội bộ trong System_Settings.
    @Transactional(readOnly = true)
    @Cacheable("public-settings")
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
    @CacheEvict(value = "public-settings", allEntries = true)
    public void updateSettings(Map<String, String> payload) {
        // Marcus sửa: chuẩn hóa key trước khi query và save để service luôn xử lý
        // nhất quán, kể cả khi được gọi nội bộ không đi qua validation controller.
        Map<String, String> normalizedPayload = payload.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey() == null ? "" : entry.getKey().trim().toUpperCase(),
                        Map.Entry::getValue,
                        (first, duplicate) -> duplicate,
                        java.util.LinkedHashMap::new));
        List<SystemSetting> existingSettings = repository.findAllById(normalizedPayload.keySet());

        Map<String, SystemSetting> existingMap = existingSettings.stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));

        List<SystemSetting> toSave = new ArrayList<>();

        for (Map.Entry<String, String> entry : normalizedPayload.entrySet()) {
            String key = entry.getKey();
            String value = validateSettingValue(key, entry.getValue());

            // Bỏ qua nếu giá trị null
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
            setting.setUpdatedBy(SecurityUtils.getCurrentUsername());
            setting.setUpdatedAt(LocalDateTime.now());
            toSave.add(setting);
        }

        repository.saveAll(toSave);
    }

    // Marcus thêm: khôi phục bộ mặc định có kiểm soát, vẫn đi qua cùng validation.
    @Transactional
    @CacheEvict(value = "public-settings", allEntries = true)
    public void restoreDefaults() {
        // Marcus sửa: gọi internal method thay vì updateSettings()
        // để tránh Spring AOP self-invocation không evict được cache.
        List<SystemSetting> existingSettings = repository.findAllById(DEFAULT_SETTINGS.keySet());
        Map<String, SystemSetting> existingMap = existingSettings.stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, s -> s));

        List<SystemSetting> toSave = new ArrayList<>();
        for (Map.Entry<String, String> entry : DEFAULT_SETTINGS.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            SystemSetting setting = existingMap.get(key);
            if (setting != null) {
                setting.setSettingValue(value);
            } else {
                setting = new SystemSetting();
                setting.setSettingKey(key);
                setting.setSettingValue(value);
                setting.setSettingGroup(determineSettingGroup(key));
                setting.setDescription("Cấu hình mặc định khôi phục từ hệ thống");
            }
            setting.setUpdatedBy(SecurityUtils.getCurrentUsername());
            setting.setUpdatedAt(LocalDateTime.now());
            toSave.add(setting);
        }
        repository.saveAll(toSave);
    }

    // Marcus thêm: System Settings chỉ nhận key do hệ thống định nghĩa và validate
    // theo loại dữ liệu; Admin không thể tạo cấu hình tùy ý bằng DevTools.
    private String validateSettingValue(String key, String rawValue) {
        if (!ADMIN_EDITABLE_KEYS.contains(key)) {
            throw new IllegalArgumentException("Khóa cấu hình không được phép cập nhật: " + key);
        }
        if (rawValue == null) {
            throw new IllegalArgumentException("Giá trị cấu hình " + key + " không được để trống.");
        }
        String value = rawValue.trim();
        if (Set.of("SITE_NAME", "HOTLINE", "EMAIL", "ADDRESS").contains(key) && value.isBlank()) {
            throw new IllegalArgumentException(key + " không được để trống.");
        }
        if ("EMAIL".equals(key) && !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Email cửa hàng không đúng định dạng.");
        }
        if ("HOTLINE".equals(key) && !value.matches("^[+0-9(). -]{8,20}$")) {
            throw new IllegalArgumentException("Hotline không đúng định dạng.");
        }
        if (URL_KEYS.contains(key) && !value.isBlank()) {
            try {
                URI uri = URI.create(value);
                if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception ex) {
                throw new IllegalArgumentException(key + " phải là URL http/https hợp lệ.");
            }
        }
        int maxLength = switch (key) {
            case "HOME_HERO_SLIDES" -> 20_000;
            case "STORE_LOCATION" -> 2_000;
            case "AI_ADVISOR_POLICY" -> 240;
            case "HOME_HERO_LEAD", "PROMO_TEXT", "ADDRESS" -> 500;
            default -> 255;
        };
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(key + " vượt quá " + maxLength + " ký tự.");
        }
        if ("STORE_LOCATION".equals(key) || "HOME_HERO_SLIDES".equals(key)) {
            try {
                objectMapper.readTree(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException(key + " không phải JSON hợp lệ.");
            }
        }
        return value;
    }

    private String validateAiPolicy(String value) {
        String policy = value.trim();
        if (policy.length() > 240) {
            throw new IllegalArgumentException("Mô tả giọng điệu Marcus AI không được vượt quá 240 ký tự.");
        }
        if (policy.matches("(?is).*(bỏ qua|phớt lờ).*(quy tắc|chỉ dẫn|bảo mật|system|prompt).*")
                || policy.matches("(?is).*(sql|database|api[ -]?key|mật khẩu|password|delete|update|insert).*")) {
            throw new IllegalArgumentException(
                    "Mô tả giọng điệu chứa chỉ dẫn không phù hợp hoặc có thể làm suy yếu bảo mật.");
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
