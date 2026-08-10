package com.fpoly.marcusstore.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.format.annotation.DateTimeFormat;

import com.fpoly.marcusstore.dto.response.FinancialReportResponse;
import com.fpoly.marcusstore.service.FinancialService;

import lombok.RequiredArgsConstructor;
import com.fpoly.marcusstore.dto.request.FinancialReconcileRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/admin/finance-reports")
@PreAuthorize("hasAuthority('DONGTIEN_VIEW')")
@RequiredArgsConstructor
@Validated
public class FinancialController {
    private final FinancialService financialService;

    @GetMapping("/export")
    // Marcus sửa: quyền xem báo cáo không đồng nghĩa với quyền tải toàn bộ dữ liệu.
    @PreAuthorize("hasAuthority('DONGTIEN_EXPORT')")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        byte[] data = financialService.exportTransactionsToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/list")
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        // Marcus thêm: backend nhận khoảng ngày rõ ràng thay vì tải toàn bộ lịch sử.
        return ResponseEntity.ok(financialService.getFinancialReport(fromDate, toDate));
    }

    @PostMapping("/{id}/reconcile")
    @PreAuthorize("hasAuthority('DONGTIEN_EXPORT')")
    public ResponseEntity<?> reconcile(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody FinancialReconcileRequest request) {
        financialService.updateReconciliationStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }
}
