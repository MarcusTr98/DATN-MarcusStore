package com.fpoly.marcusstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackupJobService {

    private final BackupService backupService;

    // Marcus thêm: tạo file nền để request của admin không bị treo khi database lớn.
    @Async
    public void generate(String id, String username, String ipAddress) {
        backupService.generateBackup(id, username, ipAddress);
    }
}
