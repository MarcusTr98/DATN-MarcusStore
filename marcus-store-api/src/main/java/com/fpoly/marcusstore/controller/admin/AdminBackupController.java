package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.CreateBackupRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.BackupOverviewResponse;
import com.fpoly.marcusstore.dto.response.BackupRecordResponse;
import com.fpoly.marcusstore.service.BackupJobService;
import com.fpoly.marcusstore.service.BackupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBackupController {

    private final BackupService backupService;
    private final BackupJobService backupJobService;

    @GetMapping("/overview")
    public ApiResponse<BackupOverviewResponse> overview() {
        return ApiResponse.success(backupService.getOverview());
    }

    @GetMapping
    public ApiResponse<List<BackupRecordResponse>> history() {
        return ApiResponse.success(backupService.listBackups());
    }

    @PostMapping
    public ApiResponse<BackupRecordResponse> create(
            @Valid @RequestBody CreateBackupRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String username = authentication.getName();
        String ipAddress = resolveClientIp(servletRequest);
        BackupRecordResponse record = backupService.initializeBackup(
                request.getType(), request.getNote(), username, ipAddress);
        backupJobService.generate(record.getId(), username, ipAddress);
        return ApiResponse.success(record);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest servletRequest) throws IOException {
        BackupRecordResponse record = backupService.getRecord(id);
        Resource resource = backupService.getDownload(
                id, authentication.getName(), resolveClientIp(servletRequest));
        String encodedName = java.net.URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + record.getFileName()
                                + "\"; filename*=UTF-8''" + encodedName)
                .header("X-Content-Type-Options", "nosniff")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable String id,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        backupService.deleteBackup(id, authentication.getName(), resolveClientIp(servletRequest));
        return ApiResponse.success("Đã xóa file sao lưu khỏi máy chủ.");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
