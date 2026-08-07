package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.GhnCreateOrderRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.core.ShippingConfigRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Marcus thêm testcase thuộc phạm vi tích hợp GHN của Marcus:
// lần tạo lỗi phải lưu FAILED; retry thành công phải chốt đúng một tracking code.
@ExtendWith(MockitoExtension.class)
class OrderShippingServiceTest {
    @Mock GhnService ghnService;
    @Mock OrderRepository orderRepository;
    @Mock ShippingConfigRepository shippingConfigRepository;
    @Mock TransactionTemplate transactionTemplate;
    @Mock TransactionStatus transactionStatus;

    private OrderShippingService service;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderShippingService(ghnService, orderRepository, shippingConfigRepository, transactionTemplate);
        order = new Order();
        order.setOrderId(10);
        order.setOrderCode("ORD-GHN-RETRY");
        order.setOrderStatus("PACKED");
        order.setFulfillmentMethod("DELIVERY");
        order.setPaymentMethod("COD");
        order.setPaymentStatus("UNPAID");
        order.setFinalAmount(new BigDecimal("1200000"));
        order.setTotalAmount(new BigDecimal("1200000"));
        order.setRecipientName("Marcus");
        order.setRecipientPhone("0912345678");
        order.setShippingAddress("118 Cát Bi");
        order.setToDistrictId(1);
        order.setToWardCode("12345");
        order.setOrderItems(new ArrayList<>());
        order.setGhnIntegrationStatus("PENDING");
        order.setGhnRetryCount(0);

        when(orderRepository.findByIdForUpdate(10)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shippingConfigRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void failedCreateIsRecordedAndNextRetryCanSucceed() {
        when(ghnService.createOrderOnGhn(any(GhnCreateOrderRequest.class)))
                .thenThrow(new RuntimeException("GHN sandbox timeout"))
                .thenReturn("GHN-TRACK-001");

        assertThatThrownBy(() -> service.createOrRetryGhnOrder(10))
                .hasMessageContaining("timeout");
        assertThat(order.getGhnIntegrationStatus()).isEqualTo("FAILED");
        assertThat(order.getGhnLastError()).contains("timeout");

        Order retried = service.createOrRetryGhnOrder(10);
        assertThat(retried.getGhnIntegrationStatus()).isEqualTo("CREATED");
        assertThat(retried.getTrackingCode()).isEqualTo("GHN-TRACK-001");
        assertThat(retried.getGhnRetryCount()).isEqualTo(2);
        verify(ghnService, times(2)).createOrderOnGhn(any(GhnCreateOrderRequest.class));
    }
}
