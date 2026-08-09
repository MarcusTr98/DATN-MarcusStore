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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fpoly.marcusstore.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class FinancialService {
        private static final String REFUND = "REFUND";
        private static final String SUCCESS = "SUCCESS";

        private final OrderTransactionRepository transactionRepository;

        // Marcus sửa: báo cáo tài chính là sổ giao dịch nên phải giữ cả giao dịch
        // PENDING/FAILED/REFUND, kể cả khi đơn đã hủy hoặc đã được hoàn tiền.
        private List<TransactionResponse> getProcessedTransactions() {
                return getProcessedTransactions(null, null);
        }

        private List<TransactionResponse> getProcessedTransactions(LocalDate fromDate, LocalDate toDate) {
                List<OrderTransaction> rawTransactions;
                if (fromDate != null && toDate != null) {
                        if (fromDate.isAfter(toDate)) {
                                throw new IllegalArgumentException("Từ ngày không được lớn hơn đến ngày");
                        }
                        // Marcus thêm: dùng khoảng nửa mở để bao trọn 23:59:59.999999999
                        // của ngày kết thúc mà không cần ép CAST lên cột created_at.
                        LocalDateTime fromDateTime = fromDate.atStartOfDay();
                        LocalDateTime toDateTimeExclusive = toDate.plusDays(1).atStartOfDay();
                        rawTransactions = transactionRepository.findTransactionsWithOrderBetween(
                                        fromDateTime, toDateTimeExclusive);
                } else {
                        rawTransactions = transactionRepository.findAllTransactionsWithOrder();
                }

                return rawTransactions.stream()
                                .filter(t -> t.getOrder() != null)
                                .map(t -> {
                                        String note = t.getNote() != null ? t.getNote() : "";

                                        BigDecimal orderFinalAmount = t.getOrder().getFinalAmount();

                                        // Marcus sửa: giữ nguyên trạng thái thật của transaction; lệch tiền
                                        // chỉ là cảnh báo đối soát. REFUND có quy tắc số tiền riêng.
                                        if (!isRefund(t)
                                                        && orderFinalAmount != null
                                                        && t.getAmount() != null
                                                        && t.getAmount().compareTo(orderFinalAmount) != 0) {
                                                note = note.isEmpty()
                                                                ? "Cảnh báo đối soát: lệch số tiền đơn (Gốc: "
                                                                                + orderFinalAmount + ")"
                                                                : note + " | Cảnh báo đối soát: lệch tiền đơn (Gốc: "
                                                                                + orderFinalAmount + ")";
                                        }
                                        boolean amountMismatch = !isRefund(t)
                                                        && orderFinalAmount != null && t.getAmount() != null
                                                        && t.getAmount().compareTo(orderFinalAmount) != 0;
                                        String cashCategory = resolveCashCategory(t);
                                        String issue = resolveReconciliationIssue(t, amountMismatch);
                                        return TransactionResponse.builder()
                                                        .transactionId(t.getTransactionId())
                                                        .providerTransactionId(
                                                                        t.getProviderTransactionId() != null
                                                                                        ? t.getProviderTransactionId()
                                                                                        : t.getOrder().getTransactionId())
                                                        .orderCode(t.getOrder().getOrderCode())
                                                        .amount(t.getAmount())
                                                        // Marcus sửa riêng lớp báo cáo: COD của đơn tự
                                                        // nhận là thanh toán tại cửa hàng, không phải
                                                        // tiền GHN thu hộ. Không đổi transaction gốc để
                                                        // tránh ảnh hưởng luồng thanh toán hiện hữu.
                                                        .type(resolveReportTransactionType(t))
                                                        .status(t.getStatus())
                                                        .orderStatus(t.getOrder().getOrderStatus())
                                                        .paymentStatus(t.getOrder().getPaymentStatus())
                                                        .fulfillmentMethod(t.getOrder().getFulfillmentMethod())
                                                        .note(note)
                                                        .createdAt(t.getCreatedAt())
                                                        .recipientName(t.getOrder().getRecipientName())
                                                        .recipientPhone(t.getOrder().getRecipientPhone())
                                                        .shippingAddress(t.getOrder().getShippingAddress())
                                                        .isReconciled(t.getIsReconciled())
                                                        .reconciledBy(t.getReconciledBy())
                                                        .reconciledAt(t.getReconciledAt())
                                                        .cashCategory(cashCategory)
                                                        .needsAttention(issue != null)
                                                        .reconciliationIssue(issue)
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
                return getFinancialReport(null, null);
        }

        public FinancialReportResponse getFinancialReport(LocalDate fromDate, LocalDate toDate) {
                List<TransactionResponse> dtoList = getProcessedTransactions(fromDate, toDate);

                // Marcus thêm: tiền vào chỉ gồm giao dịch thu thành công; hoàn tiền
                // thành công là tiền ra và phải được trừ khỏi dòng tiền ròng.
                BigDecimal successfulInflow = dtoList.stream()
                                .filter(t -> SUCCESS.equals(t.getStatus()) && !REFUND.equals(t.getType()))
                                .map(TransactionResponse::getAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal successfulRefund = dtoList.stream()
                                .filter(t -> SUCCESS.equals(t.getStatus()) && REFUND.equals(t.getType()))
                                .map(TransactionResponse::getAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal pending = dtoList.stream().filter(t -> "PENDING".equals(t.getStatus()))
                                .map(TransactionResponse::getAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal failed = dtoList.stream().filter(t -> "FAILED".equals(t.getStatus()))
                                .map(TransactionResponse::getAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal vnpayCollected = sumByCashCategory(dtoList, "VNPAY_COLLECTED");
                BigDecimal codExpected = sumByCashCategory(dtoList, "COD_EXPECTED");
                BigDecimal codReconciled = sumByCashCategory(dtoList, "COD_RECONCILED");
                BigDecimal storeCollected = sumByCashCategory(dtoList, "STORE_COLLECTED");
                BigDecimal refundExpected = sumByCashCategory(dtoList, "REFUND_EXPECTED");
                BigDecimal refundSuccessful = sumByCashCategory(dtoList, "REFUND_SUCCESSFUL");
                long attentionCount = dtoList.stream().filter(t -> Boolean.TRUE.equals(t.getNeedsAttention())).count();

                // Marcus thêm: doanh thu chỉ được ghi nhận khi tiền đã thu thành công
                // và đơn đã hoàn tất. Đây là cùng quy tắc mà Analytics/AI sử dụng.
                BigDecimal recognizedRevenue = dtoList.stream()
                                .filter(t -> SUCCESS.equals(t.getStatus())
                                                && !REFUND.equals(t.getType())
                                                && "COMPLETED".equals(t.getOrderStatus()))
                                .map(TransactionResponse::getAmount)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Marcus thêm: tiền của đơn đã hủy vẫn đang nằm trong dòng tiền cho đến
                // khi refund SUCCESS; gom theo đơn để không trừ nhầm refund của đơn khác.
                Map<String, BigDecimal> cancelledBalances = dtoList.stream()
                                .filter(t -> "CANCELLED".equals(t.getOrderStatus())
                                                && SUCCESS.equals(t.getStatus()))
                                .collect(Collectors.groupingBy(
                                                TransactionResponse::getOrderCode,
                                                Collectors.reducing(
                                                                BigDecimal.ZERO,
                                                                t -> REFUND.equals(t.getType())
                                                                                ? t.getAmount().negate()
                                                                                : t.getAmount(),
                                                                BigDecimal::add)));
                BigDecimal unsettledCancellationAmount = cancelledBalances.values().stream()
                                .filter(balance -> balance.signum() > 0)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                long successCount = dtoList.stream().filter(t -> SUCCESS.equals(t.getStatus())).count();
                double rate = dtoList.isEmpty() ? 0 : (double) successCount / dtoList.size() * 100;

                return new FinancialReportResponse(
                                dtoList,
                                dtoList.size(),
                                successfulInflow,
                                successfulRefund,
                                successfulInflow.subtract(successfulRefund),
                                recognizedRevenue,
                                unsettledCancellationAmount,
                                pending,
                                failed,
                                vnpayCollected,
                                codExpected,
                                codReconciled,
                                storeCollected,
                                refundExpected,
                                refundSuccessful,
                                attentionCount,
                                rate);
        }

        private static boolean isRefund(OrderTransaction transaction) {
                return REFUND.equalsIgnoreCase(transaction.getType());
        }

        private static String resolveReportTransactionType(OrderTransaction transaction) {
                if ("COD_COLLECTION".equalsIgnoreCase(transaction.getType())
                                && transaction.getOrder() != null
                                && "STORE_PICKUP".equalsIgnoreCase(
                                                transaction.getOrder().getFulfillmentMethod())) {
                        return "STORE_PAYMENT";
                }
                return transaction.getType();
        }

        private static String resolveCashCategory(OrderTransaction transaction) {
                String type = resolveReportTransactionType(transaction);
                String status = transaction.getStatus() == null ? "" : transaction.getStatus().toUpperCase();
                if (REFUND.equalsIgnoreCase(type)) {
                        return SUCCESS.equals(status) ? "REFUND_SUCCESSFUL" : "REFUND_EXPECTED";
                }
                if ("VNPAY_PAYMENT".equalsIgnoreCase(type) && SUCCESS.equals(status))
                        return "VNPAY_COLLECTED";
                if ("STORE_PAYMENT".equalsIgnoreCase(type) && SUCCESS.equals(status))
                        return "STORE_COLLECTED";
                if ("COD_COLLECTION".equalsIgnoreCase(type)) {
                        if (!SUCCESS.equals(status))
                                return "COD_EXPECTED";
                        return Boolean.TRUE.equals(transaction.getIsReconciled())
                                        ? "COD_RECONCILED"
                                        : "COD_AWAITING_RECONCILIATION";
                }
                return "OTHER";
        }

        private static String resolveReconciliationIssue(OrderTransaction transaction, boolean amountMismatch) {
                if (amountMismatch)
                        return "Số tiền giao dịch lệch với giá trị đơn hàng";
                if (SUCCESS.equalsIgnoreCase(transaction.getStatus())
                                && !Boolean.TRUE.equals(transaction.getIsReconciled())) {
                        return "Giao dịch thành công nhưng chưa được đối soát";
                }
                if (REFUND.equalsIgnoreCase(transaction.getType())
                                && !SUCCESS.equalsIgnoreCase(transaction.getStatus())) {
                        return "Refund chưa hoàn tất, cần tiếp tục theo dõi";
                }
                return null;
        }

        private static BigDecimal sumByCashCategory(List<TransactionResponse> transactions, String category) {
                return transactions.stream().filter(item -> category.equals(item.getCashCategory()))
                                .map(TransactionResponse::getAmount).filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public void updateReconciliationStatus(Integer transactionId, boolean status) {
                OrderTransaction tx = transactionRepository.findById(transactionId)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch"));
                // Marcus sửa: đối soát là xác nhận dòng tiền đã phát sinh, vì vậy
                // request trực tiếp cũng không được tích PENDING/FAILED.
                if (!"SUCCESS".equalsIgnoreCase(tx.getStatus())) {
                        throw new IllegalStateException("Chỉ giao dịch thành công mới được đối soát");
                }
                tx.setIsReconciled(status);
                tx.setReconciledBy(status ? SecurityUtils.getCurrentUsername() : null);
                tx.setReconciledAt(status ? LocalDateTime.now() : null);
                transactionRepository.save(tx);
        }
}
