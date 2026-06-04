package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.cms.SystemSetting;
import com.fpoly.marcusstore.repository.cms.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (existingSettings.isEmpty()) {
            throw new RuntimeException("Không tìm thấy cấu hình nào hợp lệ để cập nhật");
        }
        for (SystemSetting setting : existingSettings) {
            String newValue = payload.get(setting.getSettingKey());
            setting.setSettingValue(newValue);
        }
        repository.saveAll(existingSettings);
    }
}