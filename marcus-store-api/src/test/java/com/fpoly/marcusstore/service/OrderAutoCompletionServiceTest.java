package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderAutoCompletionServiceTest {

    private OrderRepository orderRepository;
    private OrderStatusHistoryRepository historyRepository;
    private OrderPaymentService paymentService;
    private UserNotificationService userNotificationService;
    private AdminNotificationService adminNotificationService;
    private OrderAutoCompletionService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        historyRepository = mock(OrderStatusHistoryRepository.class);
        paymentService = mock(OrderPaymentService.class);
        userNotificationService = mock(UserNotificationService.class);
        adminNotificationService = mock(AdminNotificationService.class);
        service = new OrderAutoCompletionService(
                orderRepository,
                historyRepository,
                paymentService,
                userNotificationService,
                adminNotificationService);
    }

    @Test
    void completesDeliveredDeliveryOrderOnce() {
        Order order = order("DELIVERED", "DELIVERY", "COD", "PAID");
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));

        assertThat(service.completeDeliveredOrder(1)).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo("COMPLETED");
        verify(paymentService).handlePaymentSuccess(order, "COD_COLLECTION", "AUTO_COMPLETED_AFTER_DELIVERED:ORD-1");
        verify(historyRepository).save(any());
        verify(userNotificationService).createOrderStatusNotification(eq(order), eq("COMPLETED"), anyString());
        verify(adminNotificationService).createAndSendNotification(
                eq("ORDER_COMPLETED"), anyString(), anyString(), eq("ORD-1"));
    }

    @Test
    void neverAutoCompletesStorePickup() {
        Order order = order("DELIVERED", "STORE_PICKUP", "COD", "PAID");
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));

        assertThat(service.completeDeliveredOrder(1)).isFalse();
        verifyNoInteractions(paymentService, historyRepository, userNotificationService, adminNotificationService);
    }

    @Test
    void skipsUnpaidVnPayOrder() {
        Order order = order("DELIVERED", "DELIVERY", "VNPAY", "UNPAID");
        when(orderRepository.findByIdForUpdate(1)).thenReturn(Optional.of(order));

        assertThat(service.completeDeliveredOrder(1)).isFalse();
        verifyNoInteractions(paymentService, historyRepository, userNotificationService, adminNotificationService);
    }

    private Order order(String status, String fulfillmentMethod, String paymentMethod, String paymentStatus) {
        Order order = new Order();
        order.setOrderId(1);
        order.setOrderCode("ORD-1");
        order.setOrderStatus(status);
        order.setFulfillmentMethod(fulfillmentMethod);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(paymentStatus);
        return order;
    }
}
