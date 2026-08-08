package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Marcus thêm test hồi quy cho đơn VNPAY bị bỏ dở và trường hợp IPN đã về kịp.
class VnPayPaymentExpiryServiceTest {

    private OrderRepository orderRepository;
    private OrderTransactionRepository transactionRepository;
    private OrderStatusHistoryRepository historyRepository;
    private OrderCancellationService cancellationService;
    private VnPayPaymentExpiryService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        transactionRepository = mock(OrderTransactionRepository.class);
        historyRepository = mock(OrderStatusHistoryRepository.class);
        cancellationService = mock(OrderCancellationService.class);
        service = new VnPayPaymentExpiryService(
                orderRepository, transactionRepository, historyRepository, cancellationService,
                mock(UserNotificationService.class));
    }

    @Test
    void cancelsStillPendingPaymentAndWritesHistory() {
        Order order = pendingVnPayOrder();
        when(orderRepository.findByIdForUpdate(10)).thenReturn(Optional.of(order));
        when(transactionRepository
                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        10, "VNPAY_PAYMENT", "PENDING"))
                .thenReturn(Optional.of(new OrderTransaction()));
        when(cancellationService.cancelAndRestore(
                eq(order), eq("SYSTEM_VNPAY_EXPIRED"), eq("SYSTEM"), anyString())).thenReturn(true);

        service.cancelOneExpiredPayment(10);

        assertThat(order.getPaymentStatus()).isEqualTo("FAILED");
        verify(cancellationService).cancelAndRestore(
                eq(order), eq("SYSTEM_VNPAY_EXPIRED"), eq("SYSTEM"), contains("không hoàn tất"));
        ArgumentCaptor<OrderStatusHistory> history = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void doesNotCancelWhenIpnAlreadyMarkedOrderPaid() {
        Order order = pendingVnPayOrder();
        order.setPaymentStatus("PAID");
        when(orderRepository.findByIdForUpdate(10)).thenReturn(Optional.of(order));

        service.cancelOneExpiredPayment(10);

        verifyNoInteractions(cancellationService, historyRepository);
    }

    private Order pendingVnPayOrder() {
        Order order = new Order();
        order.setOrderId(10);
        order.setPaymentMethod("VNPAY");
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PENDING");
        return order;
    }
}
