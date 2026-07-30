package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.config.VnPayConfig;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.entity.shopping.RefundRequest;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import com.fpoly.marcusstore.repository.shopping.RefundRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundServiceTest {

        private RefundRequestRepository refundRepository;
        private OrderRepository orderRepository;
        private OrderTransactionRepository transactionRepository;
        private EmailService emailService;
        private AdminNotificationService notificationService;
        private VnPayConfig vnPayConfig;
        private RefundService refundService;

        @BeforeEach
        void setUp() {
                refundRepository = mock(RefundRequestRepository.class);
                orderRepository = mock(OrderRepository.class);
                transactionRepository = mock(OrderTransactionRepository.class);
                emailService = mock(EmailService.class);
                notificationService = mock(AdminNotificationService.class);
                vnPayConfig = mock(VnPayConfig.class);
                // Marcus cập nhật fixture để kiểm chứng tích hợp thông báo refund realtime.
                refundService = new RefundService(
                                refundRepository,
                                orderRepository,
                                transactionRepository,
                                mock(UserRepository.class),
                                emailService,
                                vnPayConfig,
                                notificationService,
                                mock(UserNotificationService.class));
                when(refundRepository.save(any(RefundRequest.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void createsFullRefundIncludingCustomerPaidShipping() {
                Order order = eligibleOrder();
                OrderTransaction payment = successfulPayment(order);
                when(transactionRepository.findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                10, "VNPAY_PAYMENT", "SUCCESS"))
                                .thenReturn(Optional.of(payment));
                when(refundRepository.findByIdempotencyKey("REFUND:10:20"))
                                .thenReturn(Optional.empty());

                RefundRequest refund = refundService.requestSystemRefundIfEligible(order, "GHN returned");

                assertThat(refund.getAmount()).isEqualByComparingTo("1000000");
                assertThat(refund.getShippingDeducted()).isEqualByComparingTo("0");
                assertThat(refund.getStatus()).isEqualTo(RefundService.PENDING_APPROVAL);
                assertThat(order.getPaymentStatus()).isEqualTo("REFUND_PENDING");
                verify(notificationService).createAndSendNotification(
                                eq("REFUND"),
                                eq("Yêu cầu hoàn tiền mới"),
                                contains("1000000 VND"),
                                eq(order.getOrderCode()));

                ArgumentCaptor<OrderTransaction> transaction = ArgumentCaptor.forClass(OrderTransaction.class);
                verify(transactionRepository).save(transaction.capture());
                assertThat(transaction.getValue().getType()).isEqualTo("REFUND");
                assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("1000000");
                assertThat(transaction.getValue().getIdempotencyKey()).isEqualTo("REFUND:10:20");
        }

        @Test
        void returnsExistingRequestWithoutCreatingAnotherTransaction() {
                Order order = eligibleOrder();
                OrderTransaction payment = successfulPayment(order);
                RefundRequest existing = new RefundRequest();
                existing.setOrder(order);
                existing.setPaymentTransaction(payment);
                existing.setStatus(RefundService.PENDING_APPROVAL);

                when(transactionRepository.findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                10, "VNPAY_PAYMENT", "SUCCESS"))
                                .thenReturn(Optional.of(payment));
                when(refundRepository.findByIdempotencyKey("REFUND:10:20"))
                                .thenReturn(Optional.of(existing));

                RefundRequest actual = refundService.requestSystemRefundIfEligible(order, "duplicate webhook");

                assertThat(actual).isSameAs(existing);
                verify(transactionRepository, never()).save(any());
                verify(orderRepository, never()).save(any());
        }

        @Test
        void rejectsRefundAmountGreaterThanSuccessfulPayment() {
                Order order = eligibleOrder();
                OrderTransaction payment = successfulPayment(order);
                payment.setAmount(new BigDecimal("999999"));
                when(transactionRepository.findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                10, "VNPAY_PAYMENT", "SUCCESS"))
                                .thenReturn(Optional.of(payment));

                assertThatThrownBy(() -> refundService.requestSystemRefundIfEligible(order, "invalid amount"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("Số tiền refund không hợp lệ");
                verify(transactionRepository, never()).save(any());
        }

        @Test
        void successSynchronizesRefundOrderAndLedgerStatuses() {
                RefundRequest refund = refundFixture(RefundService.SUBMITTING);
                when(refundRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(refund));

                refundService.completeAttempt(99L,
                                VnPayRefundClient.RefundGatewayResult.success(
                                                "00", "00", "Success", "RESP-1", "REFUND-TXN-1"));

                assertThat(refund.getStatus()).isEqualTo(RefundService.SUCCESS);
                assertThat(refund.getOrder().getPaymentStatus()).isEqualTo("REFUNDED");
                assertThat(refund.getRefundTransaction().getStatus()).isEqualTo("SUCCESS");
                assertThat(refund.getRefundTransaction().getIsReconciled()).isTrue();
                assertThat(refund.getRefundTransaction().getProviderTransactionId())
                                .isEqualTo("REFUND-TXN-1");
                assertThat(refund.getProcessedAt()).isNotNull();
        }

        @Test
        void gatewayOutcomesKeepStateMachineAndFinancialLedgerConsistent() {
                RefundRequest processing = refundFixture(RefundService.SUBMITTING);
                when(refundRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(processing));
                processing.setRefundId(101L);
                refundService.completeAttempt(101L,
                                VnPayRefundClient.RefundGatewayResult.processing(
                                                "94", "05", "Processing", "RESP-2", null));
                assertThat(processing.getStatus()).isEqualTo(RefundService.PROCESSING);
                assertThat(processing.getOrder().getPaymentStatus()).isEqualTo("REFUND_PENDING");
                assertThat(processing.getRefundTransaction().getStatus()).isEqualTo("PENDING");
                assertThat(processing.getNextReconciliationAt()).isNotNull();

                RefundRequest retryable = refundFixture(RefundService.SUBMITTING);
                retryable.setRefundId(102L);
                when(refundRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(retryable));
                refundService.completeAttempt(102L,
                                VnPayRefundClient.RefundGatewayResult.retryable("Connection refused"));
                assertThat(retryable.getStatus()).isEqualTo(RefundService.RETRY_PENDING);
                assertThat(retryable.getOrder().getPaymentStatus()).isEqualTo("REFUND_PENDING");
                assertThat(retryable.getNextRetryAt()).isNotNull();

                RefundRequest failed = refundFixture(RefundService.SUBMITTING);
                failed.setRefundId(103L);
                when(refundRepository.findByIdForUpdate(103L)).thenReturn(Optional.of(failed));
                refundService.completeAttempt(103L,
                                VnPayRefundClient.RefundGatewayResult.failed("24", "09", "Rejected"));
                assertThat(failed.getStatus()).isEqualTo(RefundService.FAILED);
                assertThat(failed.getOrder().getPaymentStatus()).isEqualTo("REFUND_FAILED");
                assertThat(failed.getRefundTransaction().getStatus()).isEqualTo("FAILED");
        }

        @Test
        void automaticTechnicalRetryResetsAllThreeStateSourcesToPending() {
                RefundRequest refund = refundFixture(RefundService.RETRY_PENDING);
                refund.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
                refund.setProcessedAt(LocalDateTime.now().minusMinutes(1));
                refund.getOrder().setPaymentStatus("REFUND_FAILED");
                refund.getRefundTransaction().setStatus("FAILED");
                refund.getRefundTransaction().setIsReconciled(true);
                when(refundRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(refund));

                VnPayRefundClient.RefundCommand command = refundService.prepareAutomaticRetry(99L);

                assertThat(command).isNotNull();
                assertThat(command.requestCode()).isEqualTo(refund.getRequestCode());
                assertThat(refund.getStatus()).isEqualTo(RefundService.SUBMITTING);
                assertThat(refund.getProcessedAt()).isNull();
                assertThat(refund.getOrder().getPaymentStatus()).isEqualTo("REFUND_PENDING");
                assertThat(refund.getRefundTransaction().getStatus()).isEqualTo("PENDING");
                assertThat(refund.getRefundTransaction().getIsReconciled()).isFalse();
        }

        @Test
        void reconciliationErrorAfterSeventyTwoHoursMovesToManualReviewWithoutLosingProviderCode() {
                RefundRequest refund = refundFixture(RefundService.PROCESSING);
                refund.setApprovedAt(LocalDateTime.now().minusHours(73));
                refund.setProviderResponseCode("94");
                refund.setProviderTransactionStatus("05");
                when(refundRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(refund));

                refundService.completeReconciliation(
                                99L, VnPayRefundClient.ReconciliationResult.error("Query timeout"));

                assertThat(refund.getStatus()).isEqualTo(RefundService.MANUAL_REVIEW);
                assertThat(refund.getOrder().getPaymentStatus()).isEqualTo("REFUND_PENDING");
                assertThat(refund.getRefundTransaction().getStatus()).isEqualTo("PENDING");
                assertThat(refund.getProviderResponseCode()).isEqualTo("94");
                assertThat(refund.getProviderTransactionStatus()).isEqualTo("05");
        }

        @Test
        void definitiveFailureCannotBeBlindlyRetried() {
                RefundRequest refund = refundFixture(RefundService.FAILED);
                when(refundRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(refund));

                assertThatThrownBy(() -> refundService.prepareAdminRetry(99L))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("lỗi kỹ thuật");
        }

        @Test
        void manualConfirmationIsLockedUnlessBothSandboxFlagsAreEnabled() {
                RefundRequest refund = refundFixture(RefundService.PROCESSING);
                when(refundRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(refund));
                when(vnPayConfig.isSandbox()).thenReturn(false);
                when(vnPayConfig.isAllowManualRefundConfirmation()).thenReturn(true);

                assertThatThrownBy(() -> refundService.confirmSandboxRefund(99L, "demo"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("đang bị khóa");

                when(vnPayConfig.isSandbox()).thenReturn(true);
                when(vnPayConfig.isAllowManualRefundConfirmation()).thenReturn(false);
                assertThatThrownBy(() -> refundService.confirmSandboxRefund(99L, "demo"))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("đang bị khóa");
        }

        @Test
        void rejectsUnknownRefundStatusFilter() {
                assertThatThrownBy(() -> refundService.getRefunds(
                                "UNKNOWN", org.springframework.data.domain.PageRequest.of(0, 10)))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("không hợp lệ");
                verify(refundRepository, never()).findPage(any(), any());
        }

        private Order eligibleOrder() {
                User user = new User();
                Order order = new Order();
                order.setOrderId(10);
                order.setOrderCode("ORD-10");
                order.setUser(user);
                order.setOrderStatus("CANCELLED");
                order.setPaymentMethod("VNPAY");
                order.setPaymentStatus("PAID");
                order.setFinalAmount(new BigDecimal("1000000"));
                order.setCustomerShippingFee(new BigDecimal("30000"));
                order.setCreatedAt(LocalDateTime.now().minusDays(1));
                return order;
        }

        private OrderTransaction successfulPayment(Order order) {
                OrderTransaction payment = new OrderTransaction();
                payment.setTransactionId(20);
                payment.setOrder(order);
                payment.setAmount(new BigDecimal("1000000"));
                payment.setType("VNPAY_PAYMENT");
                payment.setStatus("SUCCESS");
                payment.setCreatedAt(LocalDateTime.now().minusDays(1));
                payment.setProviderTransactionId("VNPAY-PAYMENT-1");
                payment.setProviderTransactionDate("20260729120000");
                return payment;
        }

        private RefundRequest refundFixture(String status) {
                Order order = eligibleOrder();
                OrderTransaction payment = successfulPayment(order);
                OrderTransaction refundTransaction = new OrderTransaction();
                refundTransaction.setTransactionId(30);
                refundTransaction.setOrder(order);
                refundTransaction.setAmount(order.getFinalAmount());
                refundTransaction.setType("REFUND");
                refundTransaction.setStatus("PENDING");
                refundTransaction.setIsReconciled(false);

                RefundRequest refund = new RefundRequest();
                refund.setRefundId(99L);
                refund.setOrder(order);
                refund.setPaymentTransaction(payment);
                refund.setRefundTransaction(refundTransaction);
                refund.setRequestCode("REQUEST-REFUND-99");
                refund.setIdempotencyKey("REFUND:10:20");
                refund.setAmount(order.getFinalAmount());
                refund.setShippingDeducted(BigDecimal.ZERO);
                refund.setReason("Test refund");
                refund.setStatus(status);
                refund.setRetryCount(1);
                refund.setMaxRetries(3);
                refund.setReconciliationAttempts(0);
                refund.setCreatedAt(LocalDateTime.now().minusMinutes(5));
                return refund;
        }
}
