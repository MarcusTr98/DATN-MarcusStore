package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import com.fpoly.marcusstore.entity.shopping.UserVoucher;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderTransactionRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final OrderTransactionRepository transactionRepository;

    // Marcus thêm hủy đơn và hoàn tài nguyên đúng một lần. Trả về false nếu đơn đã
    // được hủy trước đó.
    @Transactional
    public boolean cancelAndRestore(Order order, String reason) {
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return false;
        }

        restoreStock(order);
        restoreFlashSaleQuantity(order);
        restoreVoucher(order);
        markPendingVnPayTransactionFailed(order, reason);

        order.setOrderStatus("CANCELLED");
        orderRepository.save(order);
        return true;
    }

    private void restoreFlashSaleQuantity(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());

        for (OrderItem item : orderItems) {
            if (!Boolean.TRUE.equals(item.getIsFlashSale()) || item.getFlashSaleSlot() == null) {
                continue;
            }

            Integer slotId = item.getFlashSaleSlot().getSlotId();
            Integer skuId = item.getSku().getSkuId();

            // Khoá dòng Flash Sale để tránh hoàn soldQuantity sai khi hủy đồng thời
            flashSaleItemRepository.findForUpdate(slotId, skuId).ifPresent(flashSaleItem -> {
                int soldQuantity = flashSaleItem.getSoldQuantity() == null
                        ? 0
                        : flashSaleItem.getSoldQuantity();
                flashSaleItem.setSoldQuantity(Math.max(0, soldQuantity - item.getQuantity()));
                flashSaleItemRepository.save(flashSaleItem);
            });
        }
    }

    private void restoreStock(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        List<Integer> skuIds = orderItems.stream()
                .map(item -> item.getSku().getSkuId())
                .distinct()
                .sorted()
                .toList();

        if (skuIds.isEmpty()) {
            return;
        }

        Map<Integer, ProductSku> lockedSkus = productSkuRepository.findByIdsForUpdate(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

        for (OrderItem item : orderItems) {
            ProductSku sku = lockedSkus.get(item.getSku().getSkuId());
            if (sku != null) {
                sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity());
            }
        }

        restoreItemsToCart(order, orderItems, lockedSkus);
    }

    private void restoreItemsToCart(Order order, List<OrderItem> orderItems,
            Map<Integer, ProductSku> lockedSkus) {
        Cart cart = cartRepository.findByUserUserId(order.getUser().getUserId())
                .orElseGet(() -> createCart(order));

        for (OrderItem orderItem : orderItems) {
            ProductSku sku = lockedSkus.get(orderItem.getSku().getSkuId());
            if (sku == null) {
                continue;
            }

            CartItem cartItem = cartItemRepository
                    .findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId())
                    .orElseGet(() -> createCartItem(cart, sku));
            cartItem.setQuantity(cartItem.getQuantity() + orderItem.getQuantity());
            cartItemRepository.save(cartItem);
        }
    }

    private Cart createCart(Order order) {
        Cart cart = new Cart();
        cart.setUser(order.getUser());
        cart.setCreatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    private CartItem createCartItem(Cart cart, ProductSku sku) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setSku(sku);
        cartItem.setQuantity(0);
        return cartItem;
    }

    private void restoreVoucher(Order order) {
        if (order.getVoucher() == null) {
            return;
        }

        Voucher voucher = voucherRepository.findByIdForUpdate(order.getVoucher().getVoucherId())
                .orElse(null);
        if (voucher == null) {
            return;
        }

        int previousQuantity = voucher.getQuantity() == null ? 0 : voucher.getQuantity();
        voucher.setQuantity(previousQuantity + 1);

        LocalDateTime now = LocalDateTime.now();
        boolean isWithinUsagePeriod = (voucher.getStartDate() == null || !voucher.getStartDate().isAfter(now))
                && (voucher.getEndDate() == null || !voucher.getEndDate().isBefore(now));
        if (previousQuantity <= 0 && isWithinUsagePeriod) {
            voucher.setIsActive(true);
        }
        voucherRepository.save(voucher);

        userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), order.getUser().getUserId())
                .filter(userVoucher -> Boolean.TRUE.equals(userVoucher.getIsUsed()))
                .ifPresent(this::markVoucherUnused);
    }

    private void markVoucherUnused(UserVoucher userVoucher) {
        userVoucher.setIsUsed(false);
        userVoucher.setUsedAt(null);
        userVoucherRepository.save(userVoucher);
    }

    private void markPendingVnPayTransactionFailed(Order order, String reason) {
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            return;
        }

        transactionRepository
                .findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
                        order.getOrderId(), "VNPAY_PAYMENT", "PENDING")
                .ifPresent(transaction -> markTransactionFailed(transaction, reason));
    }

    private void markTransactionFailed(OrderTransaction transaction, String reason) {
        transaction.setStatus("FAILED");
        transaction.setNote(reason);
        transactionRepository.save(transaction);
    }
}
