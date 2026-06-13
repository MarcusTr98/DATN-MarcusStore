package com.fpoly.marcusstore.controller.admin;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.AuditLogResponseDTO;
import com.fpoly.marcusstore.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditlogController {

    @Autowired
    private AuditLogService auditLogService;

    // GET /api/admin/audit-logs
    @GetMapping
    public ApiResponse<Page<AuditLogResponseDTO>> getAll(Pageable pageable) {
        return ApiResponse.success(auditLogService.getAll(pageable));
    }

    // GET /api/admin/audit-logs/{id}
    @GetMapping("/{id}")
    public ApiResponse<AuditLogResponseDTO> getOne(@PathVariable Integer id) {
        return ApiResponse.success(auditLogService.getOne(id));
    }

    // GET /api/admin/audit-logs/export
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCsv() throws IOException {
        InputStreamResource file = new InputStreamResource(auditLogService.exportCsv());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(file);
    }
}