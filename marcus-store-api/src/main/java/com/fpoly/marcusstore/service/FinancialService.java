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

        // Lọc và kiểm định tính toàn vẹn của dữ liệu đối soát
        private List<TransactionResponse> getProcessedTransactions() {
                List<OrderTransaction> rawTransactions = transactionRepository.findAllTransactionsWithOrder();

                return rawTransactions.stream()
                                // 1. ĐIỀU KIỆN XUẤT HIỆN: Chỉ xử lý giao dịch có đơn hàng tồn tại
                                .filter(t -> t.getOrder() != null)
                                // Phải là đơn Đã thanh toán (Ví dụ: VNPay) HOẶC Đã hoàn thành/Đã giao (Ví dụ:
                                // COD)
                                .filter(t -> "PAID".equals(t.getOrder().getPaymentStatus())
                                                || "COMPLETED".equals(t.getOrder().getOrderStatus())
                                                || "DELIVERED".equals(t.getOrder().getOrderStatus()))
                                .map(t -> {
                                        String actualStatus = t.getStatus();
                                        String note = t.getNote() != null ? t.getNote() : "";

                                        BigDecimal orderFinalAmount = t.getOrder().getFinalAmount();

                                        // 2. ĐỐI CHIẾU: Cảnh báo ngay nếu số tiền giao dịch lệch với hóa đơn
                                        if (orderFinalAmount != null
                                                        && t.getAmount().compareTo(orderFinalAmount) != 0) {
                                                actualStatus = "FAILED";
                                                note = note.isEmpty() ? "Lệch số tiền (Gốc: " + orderFinalAmount + ")"
                                                                : note + " | Lệch tiền (Gốc: " + orderFinalAmount + ")";
                                        }
                                        return TransactionResponse.builder()
                                                        .transactionId(t.getTransactionId())
                                                        .providerTransactionId(
                                                                        t.getProviderTransactionId() != null
                                                                                        ? t.getProviderTransactionId()
                                                                                        : t.getOrder().getTransactionId())
                                                        .orderCode(t.getOrder().getOrderCode())
                                                        .amount(t.getAmount())
                                                        .type(t.getType())
                                                        .status(actualStatus)
                                                        .note(note)
                                                        .createdAt(t.getCreatedAt())
                                                        .recipientName(t.getOrder().getRecipientName())
                                                        .recipientPhone(t.getOrder().getRecipientPhone())
                                                        .shippingAddress(t.getOrder().getShippingAddress())
                                                        .isReconciled(t.getIsReconciled())
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        public List<TransactionResponse> getAllTransactions() {
                return getProcessedTransactions();
        }

        public byte[] exportTransactionsToExcel() throws IOException {
                // Sử dụng danh sách đã qua kiểm định để xuất Excel thay vì query DB thô
                List<TransactionResponse> transactions = getProcessedTransactions();

                try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        Sheet sheet = workbook.createSheet("Transactions");
                        Row header = sheet.createRow(0);
                        header.createCell(0).setCellValue("Mã đơn");
                        header.createCell(1).setCellValue("Loại giao dịch");
                        header.createCell(2).setCellValue("Số tiền");
                        header.createCell(3).setCellValue("Trạng thái");
                        header.createCell(4).setCellValue("Ngày tạo");

                        int rowIdx = 1;
                        for (var t : transactions) {
                                Row row = sheet.createRow(rowIdx++);
                                row.createCell(0).setCellValue(t.getOrderCode());
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
                // Đồng bộ toàn bộ thống kê dựa trên dữ liệu ĐÃ LỌC và ĐÃ CHUẨN HOÁ
                List<TransactionResponse> dtoList = getProcessedTransactions();

                BigDecimal success = dtoList.stream().filter(t -> "SUCCESS".equals(t.getStatus()))
                                .map(TransactionResponse::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal pending = dtoList.stream().filter(t -> "PENDING".equals(t.getStatus()))
                                .map(TransactionResponse::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal failed = dtoList.stream().filter(t -> "FAILED".equals(t.getStatus()))
                                .map(TransactionResponse::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                long successCount = dtoList.stream().filter(t -> "SUCCESS".equals(t.getStatus())).count();
                double rate = dtoList.isEmpty() ? 0 : (double) successCount / dtoList.size() * 100;

                return new FinancialReportResponse(dtoList, dtoList.size(), success, pending, failed, rate);
        }

        public void updateReconciliationStatus(Integer transactionId, boolean status) {
                OrderTransaction tx = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
                tx.setIsReconciled(status);
                transactionRepository.save(tx);
        }
}
