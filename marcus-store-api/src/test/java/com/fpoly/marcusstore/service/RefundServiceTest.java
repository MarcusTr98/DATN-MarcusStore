package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.auth.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefundServiceTest {

    private RefundRequestRepository refundRepository;
    private OrderRepository orderRepository;
    private OrderTransactionRepository transactionRepository;
    private EmailService emailService;
    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundRepository = mock(RefundRequestRepository.class);
        orderRepository = mock(OrderRepository.class);
        transactionRepository = mock(OrderTransactionRepository.class);
        emailService = mock(EmailService.class);
        refundService = new RefundService(
                refundRepository,
                orderRepository,
                transactionRepository,
                mock(UserRepository.class),
                emailService);
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
        return order;
    }

    private OrderTransaction successfulPayment(Order order) {
        OrderTransaction payment = new OrderTransaction();
        payment.setTransactionId(20);
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("1000000"));
        payment.setType("VNPAY_PAYMENT");
        payment.setStatus("SUCCESS");
        return payment;
    }
}
