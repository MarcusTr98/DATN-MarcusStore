package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.FinancialReportResponse;
import com.fpoly.marcusstore.dto.response.TransactionResponse;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialService {
    private final OrderTransactionRepository transactionRepository;

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAllTransactionsWithOrder()
                .stream()
                .map(t -> TransactionResponse.builder()
                        .orderCode(t.getOrder().getOrderCode())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .status(t.getStatus())
                        .note(t.getNote())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public byte[] exportTransactionsToExcel() throws IOException {
        var transactions = transactionRepository.findAllTransactionsWithOrder();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");
            // Tạo Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Mã đơn");
            header.createCell(1).setCellValue("Loại giao dịch");
            header.createCell(2).setCellValue("Số tiền");
            header.createCell(3).setCellValue("Trạng thái");
            header.createCell(4).setCellValue("Ngày tạo");

            int rowIdx = 1;
            for (var t : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(t.getOrder().getOrderCode());
                row.createCell(1).setCellValue(t.getType());
                row.createCell(2).setCellValue(t.getAmount().doubleValue());
                row.createCell(3).setCellValue(t.getStatus());
                row.createCell(4).setCellValue(t.getCreatedAt().toString());
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public FinancialReportResponse getFinancialReport() {
        var all = transactionRepository.findAllTransactionsWithOrder();

        BigDecimal success = all.stream().filter(t -> "SUCCESS".equals(t.getStatus())).map(OrderTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = all.stream().filter(t -> "PENDING".equals(t.getStatus())).map(OrderTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal failed = all.stream().filter(t -> "FAILED".equals(t.getStatus())).map(OrderTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long successCount = all.stream().filter(t -> "SUCCESS".equals(t.getStatus())).count();
        double rate = all.isEmpty() ? 0 : (double) successCount / all.size() * 100;

        return new FinancialReportResponse(all, all.size(), success, pending, failed, rate);
    }
}