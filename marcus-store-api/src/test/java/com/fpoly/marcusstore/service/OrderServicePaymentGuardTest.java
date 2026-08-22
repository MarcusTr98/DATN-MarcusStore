package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.List;

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
                                mock(OrderItemRepository.class),
                                mock(ProductItemRepository.class),
                                orderRepository,
                                mock(OrderStatusHistoryRepository.class),
                                mock(UserRepository.class),
                                mock(ProductSkuRepository.class),
                                mock(ApplicationEventPublisher.class),
                                mock(OrderShippingService.class),
                                mock(OrderPaymentService.class),
                                mock(OrderCancellationService.class),
                                mock(EmailService.class),
                                mock(CommentEvaluationRepository.class),
                                mock(AdminNotificationService.class),
                                mock(UserNotificationService.class),
                                mock(OrderAssignmentService.class));

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
                                mock(OrderItemRepository.class),
                                mock(ProductItemRepository.class),
                                orderRepository,
                                mock(OrderStatusHistoryRepository.class),
                                mock(UserRepository.class),
                                mock(ProductSkuRepository.class),
                                mock(ApplicationEventPublisher.class),
                                mock(OrderShippingService.class),
                                mock(OrderPaymentService.class),
                                cancellationService,
                                mock(EmailService.class),
                                mock(CommentEvaluationRepository.class),
                                mock(AdminNotificationService.class),
                                mock(UserNotificationService.class),
                                mock(OrderAssignmentService.class));

                UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
                request.setStatus("CANCELLED");
                request.setNote("Khách yêu cầu hủy");

                assertThatThrownBy(() -> service.updateStatusOrder("ORD-GHN", request))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("hủy vận đơn GHN trước");
                verify(cancellationService, never()).cancelAndRestore(any(), any());
                verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void imeiPreviewExcludesNormalSkuFromMixedOrder() {
                OrderRepository orderRepository = mock(OrderRepository.class);
                OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
                ProductItemRepository productItemRepository = mock(ProductItemRepository.class);
                Order order = new Order();
                order.setOrderId(1);
                order.setOrderCode("ORD-MIXED");
                when(orderRepository.findByOrderCode("ORD-MIXED")).thenReturn(Optional.of(order));

                OrderItem normalItem = orderItem(11, false, "CASE-NORMAL");
                OrderItem imeiItem = orderItem(12, true, "PHONE-IMEI");
                when(orderItemRepository.findByOrder_OrderId(1)).thenReturn(List.of(normalItem, imeiItem));
                when(productItemRepository.findAvailableBySkuId(imeiItem.getSku().getSkuId()))
                                .thenReturn(List.of());

                OrderServiceImpl service = new OrderServiceImpl(
                                orderItemRepository, productItemRepository, orderRepository,
                                mock(OrderStatusHistoryRepository.class), mock(UserRepository.class),
                                mock(ProductSkuRepository.class), mock(ApplicationEventPublisher.class),
                                mock(OrderShippingService.class), mock(OrderPaymentService.class),
                                mock(OrderCancellationService.class), mock(EmailService.class),
                                mock(CommentEvaluationRepository.class), mock(AdminNotificationService.class),
                                mock(UserNotificationService.class), mock(OrderAssignmentService.class));

                var preview = service.getImeiPreview("ORD-MIXED");

                org.assertj.core.api.Assertions.assertThat(preview)
                                .hasSize(1)
                                .first().extracting("orderItemId").isEqualTo(12);
                verify(productItemRepository, never()).findAvailableBySkuId(normalItem.getSku().getSkuId());
        }

        private OrderItem orderItem(int id, boolean managesImei, String skuCode) {
                Product product = new Product();
                product.setStatusImei(managesImei);
                product.setProductName(skuCode);
                ProductSku sku = new ProductSku();
                sku.setSkuId(id + 100);
                sku.setSkuCode(skuCode);
                sku.setProduct(product);
                OrderItem item = new OrderItem();
                item.setOrderItemId(id);
                item.setQuantity(1);
                item.setSku(sku);
                return item;
        }
}
