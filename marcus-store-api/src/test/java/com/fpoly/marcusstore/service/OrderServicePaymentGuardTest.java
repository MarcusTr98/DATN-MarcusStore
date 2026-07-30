package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Marcus thêm test: admin không thể xác nhận đơn VNPAY khi tiền chưa được IPN xác nhận.
class OrderServicePaymentGuardTest {

        @Test
        void blocksPreparingUnpaidVnPayOrder() {
                OrderRepository orderRepository = mock(OrderRepository.class);
                Order order = new Order();
                order.setOrderCode("ORD-PENDING");
                order.setOrderStatus("PENDING");
                order.setPaymentMethod("VNPAY");
                order.setPaymentStatus("PENDING");
                when(orderRepository.findByOrderCodeForUpdate("ORD-PENDING")).thenReturn(Optional.of(order));

                OrderServiceImpl service = new OrderServiceImpl(
                                orderRepository,
                                mock(OrderStatusHistoryRepository.class),
                                mock(UserRepository.class),
                                mock(ProductSkuRepository.class),
                                mock(OrderShippingService.class),
                                mock(OrderPaymentService.class),
                                mock(OrderCancellationService.class),
                                mock(EmailService.class),
                                mock(CommentEvaluationRepository.class));

                UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
                request.setStatus("CONFIRMED");

                assertThatThrownBy(() -> service.updateStatusOrder("ORD-PENDING", request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("chưa thanh toán");
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void blocksLocalCancellationWhenGhnShipmentAlreadyExists() {
                OrderRepository orderRepository = mock(OrderRepository.class);
                OrderCancellationService cancellationService = mock(OrderCancellationService.class);
                Order order = new Order();
                order.setOrderCode("ORD-GHN");
                order.setOrderStatus("PACKED");
                order.setPaymentMethod("COD");
                order.setPaymentStatus("UNPAID");
                order.setFulfillmentMethod("DELIVERY");
                order.setTrackingCode("GHN-TRACKING-1");
                when(orderRepository.findByOrderCodeForUpdate("ORD-GHN"))
                                .thenReturn(Optional.of(order));

                OrderServiceImpl service = new OrderServiceImpl(
                                orderRepository,
                                mock(OrderStatusHistoryRepository.class),
                                mock(UserRepository.class),
                                mock(ProductSkuRepository.class),
                                mock(OrderShippingService.class),
                                mock(OrderPaymentService.class),
                                cancellationService,
                                mock(EmailService.class),
                                mock(CommentEvaluationRepository.class));

                UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
                request.setStatus("CANCELLED");
                request.setNote("Khách yêu cầu hủy");

                assertThatThrownBy(() -> service.updateStatusOrder("ORD-GHN", request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("hủy vận đơn GHN trước");
                verify(cancellationService, never()).cancelAndRestore(any(), any());
                verify(orderRepository, never()).save(any(Order.class));
        }
}
