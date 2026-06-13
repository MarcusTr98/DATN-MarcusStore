package com.fpoly.marcusstore.service;
import com.fpoly.marcusstore.dto.response.AuditLogResponseDTO;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import com.fpoly.marcusstore.repository.cms.AuditLogRepository;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private AuditLogResponseDTO toResponse(AuditLog log) {
        AuditLogResponseDTO.AuditLogResponseDTOBuilder builder = AuditLogResponseDTO.builder()
                .logId(log.getLogId())
                .actionType(log.getActionType())
                .tableName(log.getTableName())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt());

        if (log.getUser() != null) {
            builder.userId(log.getUser().getUserId())
                   .username(log.getUser().getUsername())
                   .fullName(log.getUser().getFullName());
        }

        return builder.build();
    }

    // Lấy tất cả log
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> getAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    // Lấy chi tiết 1 log theo ID
    @Transactional(readOnly = true)
    public AuditLogResponseDTO getOne(Integer id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy log với id: " + id));
        return toResponse(log);
    }

    public ByteArrayInputStream exportCsv() throws IOException {
        List<AuditLog> logs = auditLogRepository.findAllForExport();

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("ID,Action,Table,Description,IP Address,Username,Full Name,Created At\n");

        // Dữ liệu
        for (AuditLog log : logs) {
            sb.append(log.getLogId()).append(",");
            sb.append(log.getActionType()).append(",");
            sb.append(log.getTableName()).append(",");
            // Thay dấu , trong description thành ; để không bị lỗi CSV
            sb.append(log.getDescription() != null ? log.getDescription().replace(",", ";") : "").append(",");
            sb.append(log.getIpAddress() != null ? log.getIpAddress() : "").append(",");
            sb.append(log.getUser() != null ? log.getUser().getUsername() : "Deleted User").append(",");
            sb.append(log.getUser() != null ? log.getUser().getFullName() : "").append(",");
            sb.append(log.getCreatedAt() != null ? log.getCreatedAt() : "").append("\n");
        }

        return new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
    }
}