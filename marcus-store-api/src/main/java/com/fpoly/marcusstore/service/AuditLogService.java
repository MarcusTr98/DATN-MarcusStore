package com.fpoly.marcusstore.service;
import com.fpoly.marcusstore.dto.response.AuditLogResponseDTO;
import com.fpoly.marcusstore.entity.interaction.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fpoly.marcusstore.repository.cms.AuditLogRepository;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter VN_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

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

    // Lấy tất cả log — có phân trang
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        //bắt buộc để Excel nhận diện đúng tiếng Việt
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);

        // Header
        writer.write("ID,Action,Table,Description,IP Address,Username,Full Name,Created At\n");

        // Dữ liệu
        for (AuditLog log : logs) {
            writer.write(log.getLogId() + ",");
            writer.write(log.getActionType() + ",");
            writer.write(log.getTableName() + ",");
            // Wrap bằng "" để tránh lỗi CSV khi có dấu phẩy trong description
            writer.write("\"" + (log.getDescription() != null ? log.getDescription().replace("\"", "'") : "") + "\",");
            writer.write((log.getIpAddress() != null ? log.getIpAddress() : "") + ",");
            writer.write((log.getUser() != null ? log.getUser().getUsername() : "Deleted User") + ",");
            writer.write("\"" + (log.getUser() != null ? log.getUser().getFullName() : "") + "\",");
            // Format ngày giờ theo chuẩn VN
            writer.write((log.getCreatedAt() != null ? log.getCreatedAt().format(VN_FORMATTER) : "") + "\n");
        }

        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }
}