package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.ProductItem;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
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
import com.fpoly.marcusstore.utils.CancellationReasonCatalog;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductItemRepository productItemRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final OrderTransactionRepository transactionRepository;
    private final RefundService refundService;

    // Marcus thêm hủy đơn và hoàn tài nguyên đúng một lần. Trả về false nếu đơn đã
    // được hủy trước đó.
    @Transactional
    public boolean cancelAndRestore(Order order, String reason) {
        return cancelAndRestore(order, null, "SYSTEM", reason);
    }

    // Marcus thêm: lưu nguồn và mã lý do hủy một lần cùng transaction hoàn tài
    // nguyên. Giữ overload cũ để không phá các điểm tích hợp hiện tại.
    @Transactional
    public boolean cancelAndRestore(Order order, String reasonCode, String actor, String reason) {
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return false;
        }

        // Marcus sửa: refund tiền và hoàn quyền dùng voucher là hai nghiệp vụ độc
        // lập. Mọi đơn hủy hợp lệ đều hoàn voucher đúng một lần.
        boolean paidByVnPay = isPaidVnPay(order);
        Map<Integer, FlashSaleItem> restorableFlashSaleContexts = restoreFlashSaleQuantity(order);
        restoreStock(order, restorableFlashSaleContexts);
        restoreImeisToStock(order);
        restoreVoucher(order);
        markPendingVnPayTransactionFailed(order, reason);

        order.setOrderStatus("CANCELLED");
        order.setCancellationActor(normalizeActor(actor));
        order.setCancellationReasonCode(CancellationReasonCatalog.normalizeCode(reasonCode, actor));
        // Marcus thêm: ghi chú nghiệp vụ chi tiết nằm ở bảng hủy riêng; timeline
        // vẫn giữ bản trình bày lịch sử cho Admin/khách hàng.
        order.setCancellationDetail(normalizeDetail(reason));
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);
        if (paidByVnPay) {
            refundService.requestSystemRefundIfEligible(order, reason);
        }
        return true;
    }

    private String normalizeActor(String actor) {
        if (actor == null) {
            return "SYSTEM";
        }
        return switch (actor.trim().toUpperCase()) {
            case "CUSTOMER", "ADMIN", "SYSTEM", "GHN" -> actor.trim().toUpperCase();
            default -> "SYSTEM";
        };
    }

    private String normalizeDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return null;
        }
        String normalized = detail.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    // Marcus thêm: IPN thành công đến sau scheduler không được hoàn kho/voucher lần
    // hai; chỉ ghi nhận đúng số tiền đã thu và tạo một refund idempotent.
    @Transactional
    public void requestRefundForCancelledPaidOrder(Order order, String reason) {
        if (order == null
                || !"CANCELLED".equalsIgnoreCase(order.getOrderStatus())
                || !isPaidVnPay(order)) {
            throw new IllegalArgumentException("Đơn hủy chưa đủ điều kiện tạo refund VNPAY");
        }
        refundService.requestSystemRefundIfEligible(order, reason);
    }

    // Marcus them refund
    private boolean isPaidVnPay(Order order) {
        return "VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                && ("PAID".equalsIgnoreCase(order.getPaymentStatus())
                        || "REFUND_PENDING".equalsIgnoreCase(order.getPaymentStatus())
                        || "REFUND_FAILED".equalsIgnoreCase(order.getPaymentStatus()));
    }

    private Map<Integer, FlashSaleItem> restoreFlashSaleQuantity(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        Map<Integer, FlashSaleItem> restorableContexts = new java.util.HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (OrderItem item : orderItems) {
            if (!Boolean.TRUE.equals(item.getIsFlashSale()) || item.getFlashSaleSlot() == null) {
                continue;
            }

            Integer slotId = item.getFlashSaleSlot().getSlotId();
            Integer skuId = item.getSku().getSkuId();

            // Khoá dòng Flash Sale để tránh hoàn soldQuantity sai khi hủy đồng thời
            flashSaleItemRepository.findForRestore(slotId, skuId).ifPresent(flashSaleItem -> {
                int soldQuantity = flashSaleItem.getSoldQuantity() == null
                        ? 0
                        : flashSaleItem.getSoldQuantity();
                flashSaleItem.setSoldQuantity(Math.max(0, soldQuantity - item.getQuantity()));
                flashSaleItemRepository.save(flashSaleItem);

                if (isFlashSaleContextStillUsable(flashSaleItem, item.getQuantity(), now)) {
                    restorableContexts.put(item.getOrderItemId(), flashSaleItem);
                }
            });
        }
        return restorableContexts;
    }

    private void restoreStock(Order order, Map<Integer, FlashSaleItem> restorableFlashSaleContexts) {
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

        restoreItemsToCart(order, orderItems, lockedSkus, restorableFlashSaleContexts);
    }

    private void restoreItemsToCart(Order order, List<OrderItem> orderItems,
            Map<Integer, ProductSku> lockedSkus,
            Map<Integer, FlashSaleItem> restorableFlashSaleContexts) {
        Cart cart = cartRepository.findByUserUserId(order.getUser().getUserId())
                .orElseGet(() -> createCart(order));

        for (OrderItem orderItem : orderItems) {
            ProductSku sku = lockedSkus.get(orderItem.getSku().getSkuId());
            if (sku == null) {
                continue;
            }

            CartItem cartItem = cartItemRepository
                    .findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId())
                    .orElse(null);
            FlashSaleItem flashSaleContext = restorableFlashSaleContexts.get(orderItem.getOrderItemId());

            if (cartItem == null) {
                cartItem = createCartItem(cart, sku, flashSaleContext);
            } else {
                preserveCompatibleFlashSaleContext(cartItem, orderItem, flashSaleContext);
            }
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

    private CartItem createCartItem(Cart cart, ProductSku sku, FlashSaleItem flashSaleContext) {
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setSku(sku);
        cartItem.setQuantity(0);
        if (flashSaleContext != null) {
            cartItem.setFlashSaleSlot(flashSaleContext.getSlot());
            cartItem.setFlashSalePrice(flashSaleContext.getFlashSalePrice());
        }
        return cartItem;
    }

    private void preserveCompatibleFlashSaleContext(
            CartItem cartItem, OrderItem orderItem, FlashSaleItem restoredFlashSaleContext) {
        if (cartItem.getFlashSaleSlot() == null) {
            return;
        }

        Integer existingSlotId = cartItem.getFlashSaleSlot().getSlotId();
        Integer restoredSlotId = orderItem.getFlashSaleSlot() == null
                ? null
                : orderItem.getFlashSaleSlot().getSlotId();
        if (restoredFlashSaleContext == null) {
            if (existingSlotId.equals(restoredSlotId)) {
                cartItem.setFlashSaleSlot(null);
                cartItem.setFlashSalePrice(null);
            }
            return;
        }

        restoredSlotId = restoredFlashSaleContext.getSlot().getSlotId();
        if (existingSlotId.equals(restoredSlotId)) {
            // Giá trong cart phải theo cấu hình Flash Sale hiện hành, không tin snapshot
            // cũ.
            cartItem.setFlashSalePrice(restoredFlashSaleContext.getFlashSalePrice());
        }
    }

    private boolean isFlashSaleContextStillUsable(
            FlashSaleItem flashSaleItem, int restoredQuantity, LocalDateTime now) {
        if (flashSaleItem.getSlot() == null
                || flashSaleItem.getFlashSaleQuantity() == null
                || flashSaleItem.getSoldQuantity() == null
                || flashSaleItem.getFlashSalePrice() == null
                || flashSaleItem.getSlot().getStartDate() == null
                || flashSaleItem.getSlot().getEndDate() == null) {
            return false;
        }

        Short status = flashSaleItem.getSlot().getStatus();
        boolean usableStatus = status != null && (status == 1 || status == 2);
        boolean withinTime = !now.isBefore(flashSaleItem.getSlot().getStartDate())
                && now.isBefore(flashSaleItem.getSlot().getEndDate());
        int remainingQuantity = flashSaleItem.getFlashSaleQuantity() - flashSaleItem.getSoldQuantity();
        boolean hasQuota = remainingQuantity >= restoredQuantity;
        return usableStatus && withinTime && hasQuota;
    }

    private void restoreImeisToStock(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
        if (orderItems.isEmpty()) {
            return;
        }

        int restoredCount = 0;
        for (OrderItem orderItem : orderItems) {
            List<ProductItem> assignedItems = orderItem.getProductItems();
            if (assignedItems == null || assignedItems.isEmpty()) {
                continue;
            }
            for (ProductItem item : assignedItems) {
                item.setOrderItem(null);
                item.setStatus(ProductItemService.STATUS_IN_STOCK);
                productItemRepository.save(item);
                restoredCount++;
            }
        }

        if (restoredCount > 0) {
            System.out.println("[OrderCancellation] Hoàn " + restoredCount
                    + " IMEI về kho cho đơn " + order.getOrderCode());
        }
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
                .findByVoucherIdAndUserIdForUpdate(voucher.getVoucherId(), order.getUser().getUserId())
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
