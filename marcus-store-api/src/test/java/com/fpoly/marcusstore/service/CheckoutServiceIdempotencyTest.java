package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CheckoutRequestDTO;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.SystemSettingRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// Marcus thêm testcase nền thuộc phạm vi Checkout của Marcus:
// hai request cùng khóa chỉ trả về một Order và không gọi lại kho/Voucher/Flash Sale.
@ExtendWith(MockitoExtension.class)
class CheckoutServiceIdempotencyTest {
    @Mock
    CartItemRepository cartItemRepository;
    @Mock
    CartRepository cartRepository;
    @Mock
    ProductSkuRepository productSkuRepository;
    @Mock
    OrderRepository orderRepository;
    @Mock
    OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    VoucherRepository voucherRepository;
    @Mock
    VoucherService voucherService;
    @Mock
    GhnService ghnService;
    @Mock
    AdminNotificationService notificationService;
    @Mock
    UserNotificationService userNotificationService;
    @Mock
    ShippingService shippingService;
    @Mock
    OrderTransactionService orderTransactionService;
    @Mock
    FlashSaleItemRepository flashSaleItemRepository;
    @Mock
    FlashSaleSlotRepository flashSaleSlotRepository;
    @Mock
    SystemSettingRepository systemSettingRepository;
    @Mock
    OrderAssignmentService orderAssignmentService;
    @InjectMocks
    CheckoutService checkoutService;

    @Test
    void replayAfterSuccessReturnsExistingOrderWithoutTouchingCartOrStock() {
        CheckoutRequestDTO request = request("checkout-double-request-0001");
        Order existing = new Order();
        existing.setOrderId(501);
        existing.setOrderCode("ORD-IDEMPOTENT");
        when(orderRepository.findByCheckoutRequestIdAndUserUserId(request.getCheckoutRequestId(), 7))
                .thenReturn(Optional.of(existing));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7);
            assertThat(checkoutService.processCheckout(request)).isSameAs(existing);
        }

        verify(cartRepository, never()).findByUserIdForCheckout(anyInt());
        verifyNoInteractions(productSkuRepository, voucherRepository, flashSaleItemRepository);
    }

    @Test
    void concurrentRequestRechecksAfterCartLockBeforeChangingResources() {
        CheckoutRequestDTO request = request("checkout-double-request-0002");
        Order existing = new Order();
        existing.setOrderCode("ORD-WINNER");
        when(orderRepository.findByCheckoutRequestIdAndUserUserId(request.getCheckoutRequestId(), 7))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(cartRepository.findByUserIdForCheckout(7)).thenReturn(Optional.of(new Cart()));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn(7);
            assertThat(checkoutService.processCheckout(request)).isSameAs(existing);
        }

        verify(cartRepository).findByUserIdForCheckout(7);
        verifyNoInteractions(productSkuRepository, voucherRepository, flashSaleItemRepository);
    }

    @Test
    void inactiveParentProductIsRejectedEvenWhenSkuIsStillActive() {
        Product product = new Product();
        product.setStatus(false);
        ProductSku sku = new ProductSku();
        sku.setSkuCode("SKU-PARENT-OFF");
        sku.setIsActive(true);
        sku.setProduct(product);

        assertThatThrownBy(() -> checkoutService.validateSkuSellable(sku, sku.getSkuCode()))
                .hasMessageContaining("PRODUCT_INACTIVE");
    }

    @Test
    void fourthPendingCodOrderIsRejectedButVnPayIsNotCounted() {
        ReflectionTestUtils.setField(checkoutService, "maxPendingCodOrders", 3L);
        when(orderRepository.countPendingCodOrders(7)).thenReturn(3L);

        assertThatThrownBy(() -> checkoutService.validateCodPendingLimit("COD", 7))
                .hasMessageContaining("COD_PENDING_LIMIT");

        checkoutService.validateCodPendingLimit("VNPAY", 7);
        verify(orderRepository, times(1)).countPendingCodOrders(7);
    }

    private CheckoutRequestDTO request(String requestId) {
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setCheckoutRequestId(requestId);
        return request;
    }
}
