package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Marcus thêm: kiểm chứng scheduler COD chỉ hủy đúng đơn còn PENDING và không
// hoàn tài nguyên lại khi trạng thái đã được Admin thay đổi.
class CodPendingExpiryServiceTest {
    private OrderRepository orderRepository;
    private OrderStatusHistoryRepository historyRepository;
    private OrderCancellationService cancellationService;
    private UserNotificationService userNotificationService;
    private CodPendingExpiryService service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        historyRepository = mock(OrderStatusHistoryRepository.class);
        cancellationService = mock(OrderCancellationService.class);
        userNotificationService = mock(UserNotificationService.class);
        service = new CodPendingExpiryService(
                orderRepository, historyRepository, cancellationService, userNotificationService);
    }

    @Test
    void expiredPendingCodIsCancelledAndNotifiedExactlyOnce() {
        Order order = pendingCod();
        when(orderRepository.findByIdForUpdate(12)).thenReturn(Optional.of(order));
        when(cancellationService.cancelAndRestore(
                order, CodPendingExpiryService.REASON_CODE, "SYSTEM", CodPendingExpiryService.REASON))
                .thenReturn(true);

        assertThat(service.cancelOneExpiredCod(12)).isTrue();
        verify(historyRepository).save(any());
        verify(userNotificationService).createOrderStatusNotification(
                eq(order), eq("CANCELLED"), contains(order.getOrderCode()));
    }

    @Test
    void confirmedOrderIsSkippedWhenAdminWonTheRace() {
        Order order = pendingCod();
        order.setOrderStatus("CONFIRMED");
        when(orderRepository.findByIdForUpdate(12)).thenReturn(Optional.of(order));

        assertThat(service.cancelOneExpiredCod(12)).isFalse();
        verifyNoInteractions(cancellationService, historyRepository, userNotificationService);
    }

    private Order pendingCod() {
        User user = new User();
        user.setUserId(7);
        Order order = new Order();
        order.setOrderId(12);
        order.setOrderCode("ORD-COD-EXPIRED");
        order.setUser(user);
        order.setPaymentMethod("COD");
        order.setPaymentStatus("UNPAID");
        order.setOrderStatus("PENDING");
        return order;
    }
}
