package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.entity.shopping.UserVoucher;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCancellationServiceTest {

        private OrderRepository orderRepository;
        private OrderItemRepository orderItemRepository;
        private ProductSkuRepository productSkuRepository;
        private ProductItemRepository productItemRepository;
        private FlashSaleItemRepository flashSaleItemRepository;
        private CartRepository cartRepository;
        private CartItemRepository cartItemRepository;
        private VoucherRepository voucherRepository;
        private UserVoucherRepository userVoucherRepository;
        private OrderTransactionRepository transactionRepository;
        private RefundService refundService;
        private OrderCancellationService cancellationService;

        @BeforeEach
        void setUp() {
                orderRepository = mock(OrderRepository.class);
                orderItemRepository = mock(OrderItemRepository.class);
                productSkuRepository = mock(ProductSkuRepository.class);
                productItemRepository = mock(ProductItemRepository.class);
                flashSaleItemRepository = mock(FlashSaleItemRepository.class);
                cartRepository = mock(CartRepository.class);
                cartItemRepository = mock(CartItemRepository.class);
                voucherRepository = mock(VoucherRepository.class);
                userVoucherRepository = mock(UserVoucherRepository.class);
                transactionRepository = mock(OrderTransactionRepository.class);
                refundService = mock(RefundService.class);

                cancellationService = new OrderCancellationService(
                                orderRepository,
                                orderItemRepository,
                                productSkuRepository,
                                productItemRepository,
                                flashSaleItemRepository,
                                cartRepository,
                                cartItemRepository,
                                voucherRepository,
                                userVoucherRepository,
                                transactionRepository,
                                refundService);
        }

        @Test
        void cancelCodRestoresStockVoucherAndResourcesOnlyOnce() {
                Fixture fixture = fixture("COD", "UNPAID");

                boolean first = cancellationService.cancelAndRestore(fixture.order(), "Khách đổi ý");
                boolean second = cancellationService.cancelAndRestore(fixture.order(), "Callback lặp");

                assertThat(first).isTrue();
                assertThat(second).isFalse();
                assertThat(fixture.order().getOrderStatus()).isEqualTo("CANCELLED");
                assertThat(fixture.sku().getStockQuantity()).isEqualTo(6);
                assertThat(fixture.voucher().getQuantity()).isEqualTo(1);
                assertThat(fixture.voucher().getIsActive()).isTrue();
                assertThat(fixture.userVoucher().getIsUsed()).isFalse();
                assertThat(fixture.userVoucher().getUsedAt()).isNull();

                verify(orderRepository, times(1)).save(fixture.order());
                verify(voucherRepository, times(1)).save(fixture.voucher());
                verify(refundService, never()).requestSystemRefundIfEligible(any(), any());
        }

        @Test
        void cancelUnpaidVnPayFailsPendingTransactionAndDoesNotCreateRefund() {
                Fixture fixture = fixture("VNPAY", "PENDING");
                OrderTransaction pendingPayment = new OrderTransaction();
                pendingPayment.setStatus("PENDING");
                when(transactionRepository
                                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                                fixture.order().getOrderId(), "VNPAY_PAYMENT", "PENDING"))
                                .thenReturn(Optional.of(pendingPayment));

                cancellationService.cancelAndRestore(fixture.order(), "Khách thoát cổng VNPAY");

                assertThat(pendingPayment.getStatus()).isEqualTo("FAILED");
                assertThat(fixture.voucher().getQuantity()).isEqualTo(1);
                verify(transactionRepository).save(pendingPayment);
                verify(refundService, never()).requestSystemRefundIfEligible(any(), any());
        }

        @Test
        void cancelPaidVnPayRestoresVoucherAndCreatesExactlyOneRefundRequest() {
                Fixture fixture = fixture("VNPAY", "PAID");

                boolean first = cancellationService.cancelAndRestore(fixture.order(), "Admin hủy đơn");
                boolean second = cancellationService.cancelAndRestore(fixture.order(), "Yêu cầu gửi lại");

                assertThat(first).isTrue();
                assertThat(second).isFalse();
                assertThat(fixture.sku().getStockQuantity()).isEqualTo(6);
                assertThat(fixture.voucher().getQuantity()).isEqualTo(1);
                assertThat(fixture.userVoucher().getIsUsed()).isFalse();
                verify(refundService, times(1))
                                .requestSystemRefundIfEligible(fixture.order(), "Admin hủy đơn");
        }

        private Fixture fixture(String paymentMethod, String paymentStatus) {
                User user = new User();
                user.setUserId(7);

                Voucher voucher = new Voucher();
                voucher.setVoucherId(3);
                voucher.setQuantity(0);
                voucher.setIsActive(false);
                voucher.setStartDate(LocalDateTime.now().minusDays(1));
                voucher.setEndDate(LocalDateTime.now().plusDays(1));

                UserVoucher userVoucher = new UserVoucher();
                userVoucher.setVoucher(voucher);
                userVoucher.setUser(user);
                userVoucher.setIsUsed(true);
                userVoucher.setUsedAt(LocalDateTime.now().minusMinutes(5));

                ProductSku sku = new ProductSku();
                sku.setSkuId(5);
                sku.setStockQuantity(4);

                Order order = new Order();
                order.setOrderId(10);
                order.setOrderCode("ORD-CANCEL-10");
                order.setOrderStatus("PENDING");
                order.setPaymentMethod(paymentMethod);
                order.setPaymentStatus(paymentStatus);
                order.setUser(user);
                order.setVoucher(voucher);

                OrderItem orderItem = new OrderItem();
                orderItem.setOrderItemId(100);
                orderItem.setOrder(order);
                orderItem.setSku(sku);
                orderItem.setQuantity(2);
                orderItem.setIsFlashSale(false);

                Cart cart = new Cart();
                cart.setCartId(8);
                cart.setUser(user);

                when(orderItemRepository.findByOrder_OrderId(order.getOrderId()))
                                .thenReturn(List.of(orderItem));
                when(productSkuRepository.findByIdsForUpdate(List.of(sku.getSkuId())))
                                .thenReturn(List.of(sku));
                when(cartRepository.findByUserUserId(user.getUserId()))
                                .thenReturn(Optional.of(cart));
                when(cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId()))
                                .thenReturn(Optional.empty());
                when(cartItemRepository.save(any(CartItem.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
                when(voucherRepository.findByIdForUpdate(voucher.getVoucherId()))
                                .thenReturn(Optional.of(voucher));
                when(userVoucherRepository.findByVoucherIdAndUserIdForUpdate(
                                voucher.getVoucherId(), user.getUserId()))
                                .thenReturn(Optional.of(userVoucher));
                when(transactionRepository
                                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                                                order.getOrderId(), "VNPAY_PAYMENT", "PENDING"))
                                .thenReturn(Optional.empty());

                return new Fixture(order, sku, voucher, userVoucher);
        }

        private record Fixture(
                        Order order,
                        ProductSku sku,
                        Voucher voucher,
                        UserVoucher userVoucher) {
        }
}