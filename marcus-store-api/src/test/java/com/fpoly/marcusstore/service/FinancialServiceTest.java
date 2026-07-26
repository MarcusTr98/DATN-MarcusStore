package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.FinancialReportResponse;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialServiceTest {

        private OrderTransactionRepository transactionRepository;
        private FinancialService financialService;

        @BeforeEach
        void setUp() {
                transactionRepository = mock(OrderTransactionRepository.class);
                financialService = new FinancialService(transactionRepository);
        }

        @Test
        void includesRefundedOrderAndSubtractsSuccessfulRefundFromNetCashFlow() {
                Order refundedOrder = order("ORD-REFUND", "CANCELLED", "REFUNDED", "1000000");
                OrderTransaction payment = transaction(
                                refundedOrder, "VNPAY_PAYMENT", "SUCCESS", "1000000", 1);
                OrderTransaction refund = transaction(
                                refundedOrder, "REFUND", "SUCCESS", "1000000", 2);
                when(transactionRepository.findAllTransactionsWithOrder())
                                .thenReturn(List.of(payment, refund));

                FinancialReportResponse report = financialService.getFinancialReport();

                // Marcus thêm: payment là tiền vào, refund là tiền ra; dòng tiền ròng bằng 0.
                assertThat(report.transactions()).hasSize(2);
                assertThat(report.totalSuccessAmount()).isEqualByComparingTo("1000000");
                assertThat(report.totalRefundAmount()).isEqualByComparingTo("1000000");
                assertThat(report.netCashFlow()).isEqualByComparingTo("0");
        }

        @Test
        void keepsRealTransactionStatusWhenPaymentAmountNeedsReconciliation() {
                Order order = order("ORD-MISMATCH", "COMPLETED", "PAID", "1000000");
                OrderTransaction payment = transaction(
                                order, "VNPAY_PAYMENT", "SUCCESS", "900000", 3);
                when(transactionRepository.findAllTransactionsWithOrder())
                                .thenReturn(List.of(payment));

                FinancialReportResponse report = financialService.getFinancialReport();

                // Marcus thêm: cảnh báo đối soát không được làm sai trạng thái thật của giao
                // dịch.
                assertThat(report.transactions().getFirst().getStatus()).isEqualTo("SUCCESS");
                assertThat(report.transactions().getFirst().getNote()).contains("Cảnh báo đối soát");
        }

        @Test
        void queriesTheWholeEndDateWithoutCastingCreatedAt() {
                LocalDate fromDate = LocalDate.of(2026, 7, 19);
                LocalDate toDate = LocalDate.of(2026, 7, 25);
                when(transactionRepository.findTransactionsWithOrderBetween(
                                fromDate.atStartOfDay(),
                                toDate.plusDays(1).atStartOfDay()))
                                .thenReturn(List.of());

                financialService.getFinancialReport(fromDate, toDate);

                // Marcus thêm: ngày kết thúc dùng đầu ngày kế tiếp làm cận loại trừ.
                verify(transactionRepository).findTransactionsWithOrderBetween(
                                LocalDateTime.of(2026, 7, 19, 0, 0),
                                LocalDateTime.of(2026, 7, 26, 0, 0));
        }

        private Order order(
                        String orderCode,
                        String orderStatus,
                        String paymentStatus,
                        String finalAmount) {
                Order order = new Order();
                order.setOrderCode(orderCode);
                order.setOrderStatus(orderStatus);
                order.setPaymentStatus(paymentStatus);
                order.setFinalAmount(new BigDecimal(finalAmount));
                order.setTransactionId("PROVIDER-" + orderCode);
                return order;
        }

        private OrderTransaction transaction(
                        Order order,
                        String type,
                        String status,
                        String amount,
                        int id) {
                return OrderTransaction.builder()
                                .transactionId(id)
                                .order(order)
                                .type(type)
                                .status(status)
                                .amount(new BigDecimal(amount))
                                .note("")
                                .createdAt(LocalDateTime.now())
                                .isReconciled(false)
                                .build();
        }
}
