package com.fpoly.marcusstore.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.fpoly.marcusstore.dto.response.FinancialReportResponse;
import com.fpoly.marcusstore.service.FinancialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/finance-reports")
@RequiredArgsConstructor
public class FinancialController {
    private final FinancialService financialService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() throws IOException {
        byte[] data = financialService.exportTransactionsToExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getTransactionList() {
        return ResponseEntity.ok(financialService.getAllTransactions());
    }

    @GetMapping("/list")
    public ResponseEntity<FinancialReportResponse> getFinancialReport() {
        return ResponseEntity.ok(financialService.getFinancialReport());
    }
}
