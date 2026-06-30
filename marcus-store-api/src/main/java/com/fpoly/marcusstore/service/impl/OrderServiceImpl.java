package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.OrderPaymentService;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.OrderShippingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final OrderShippingService orderShippingService;
    private final OrderPaymentService orderPaymentService;

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank() || "ALL".equalsIgnoreCase(paymentMethod) ? null
                : paymentMethod.trim();
    }

    private String normalizeOrderStatus(String orderStatus) {
        return orderStatus == null || orderStatus.isBlank() || "ALL".equalsIgnoreCase(orderStatus) ? null
                : orderStatus.trim();
    }

    private String normalizeStatusValue(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private String getUserDisplayName(User user) {
        if (user == null)
            return null;
        if (user.getFullName() != null && !user.getFullName().isBlank())
            return user.getFullName();
        return user.getUsername();
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderCode(order.getOrderCode())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .itemCount(orderRepository.countItemsByOrderId(order.getOrderId()))
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt()).build();
    }

    private boolean canChangeStatus(String currentStatus, String newStatus) {
        return switch (currentStatus) {
            case "PENDING" -> newStatus.equals("CONFIRMED") || newStatus.equals("CANCELLED");
            case "CONFIRMED" -> newStatus.equals("PROCESSING") || newStatus.equals("CANCELLED");
            case "PROCESSING" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
            case "SHIPPING" -> newStatus.equals("COMPLETED") || newStatus.equals("FAILED");
            case "FAILED" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
            default -> false;
        };
    }

    private boolean requiresNote(String status) {
        return "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private String getHistoryTitle(String status) {
        return switch (status) {
            case "PENDING" -> "Đơn hàng đã đặt";
            case "CONFIRMED" -> "Đơn hàng đã được xác nhận";
            case "PROCESSING" -> "Đơn hàng đang được chuẩn bị";
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "COMPLETED" -> "Giao hàng thành công";
            case "FAILED" -> "Giao hàng không thành công";
            case "CANCELLED" -> "Đơn hàng đã hủy";
            default -> "Cập nhật trạng thái";
        };
    }

    private void markPaymentPaidWhenCompleted(Order order) {
        if ("COMPLETED".equals(normalizeStatusValue(order.getOrderStatus()))) {
            order.setPaymentStatus("PAID");
        }
    }

    private OrderStatusHistory createStatusHistory(Order order, String status, String note) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.getReferenceById(currentUserId);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setTitle(getHistoryTitle(status));
        history.setNote(note);
        history.setCreatedBy(currentUser);
        return history;
    }

    @Override
    @Transactional
    public OrderDetailResponse updateStatusOrder(String orderCode, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        String currentStatus = normalizeStatusValue(order.getOrderStatus());

        String newStatus = request.getStatus();
        if (newStatus == null || newStatus.isBlank()) {
            throw new RuntimeException("Trạng thái mới không hợp lệ");
        }
        newStatus = normalizeStatusValue(newStatus);

        boolean isJustConfirmed = "PENDING".equals(currentStatus) && "CONFIRMED".equals(newStatus);

        if (!canChangeStatus(currentStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        String note = request.getNote();

        if (requiresNote(newStatus) && (note == null || note.isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do cho trạng thái này");
        }

        boolean wasCancelled = "CANCELLED".equals(newStatus);

        if (wasCancelled) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());
            List<Integer> skuIds = orderItems.stream()
                    .map(item -> item.getSku().getSkuId())
                    .toList();
            List<ProductSku> lockedSkus = productSkuRepository.findByIdsForUpdate(skuIds);
            Map<Integer, ProductSku> skuMap = lockedSkus.stream()
                    .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

            for (OrderItem item : orderItems) {
                ProductSku sku = skuMap.get(item.getSku().getSkuId());
                if (sku != null) {
                    sku.setStockQuantity(sku.getStockQuantity() + item.getQuantity());
                }
            }

            if (order.getVoucher() != null) {
                Voucher voucher = order.getVoucher();
                LocalDateTime now = LocalDateTime.now();

                // Chỉ hoàn quota nếu voucher còn hiệu lực (chưa hết hạn)
                if (voucher.getEndDate() == null || voucher.getEndDate().isAfter(now)) {
                    voucher.setQuantity(voucher.getQuantity() + 1);
                    voucherRepository.save(voucher);
                }

                // Reset UserVoucher.isUsed = false
                Integer userId = order.getUser().getUserId();
                userVoucherRepository
                        .findByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), userId)
                        .ifPresent(userVoucher -> {
                            if (Boolean.TRUE.equals(userVoucher.getIsUsed())) {
                                userVoucher.setIsUsed(false);
                                userVoucher.setUsedAt(null);
                                userVoucherRepository.save(userVoucher);
                            }
                        });
            }
        }

        order.setOrderStatus(newStatus);
        markPaymentPaidWhenCompleted(order);
        orderRepository.save(order);

        OrderStatusHistory history = createStatusHistory(order, newStatus, note);
        orderStatusHistoryRepository.save(history);

        if (isJustConfirmed) {
            orderShippingService.processCreateGhnOrder(order);
        }

        return getOrderDetailResponse(orderCode);
    }

    @Override
    public Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus,
            Pageable pageable) {
        return orderRepository.searchOrders(
                normalizeKeyword(keyword), normalizePaymentMethod(paymentMethod), normalizeOrderStatus(orderStatus),
                pageable)
                .map(this::toResponse);
    }

    @Override
    public OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus) {
        String kw = normalizeKeyword(keyword);
        String pm = normalizePaymentMethod(paymentMethod);
        String os = normalizeOrderStatus(orderStatus);

        return OrderStatsResponse.builder()
                .total(orderRepository.countOrders(kw, pm, os))
                .pending(orderRepository.countPendingOrders(kw, pm, os))
                .confirmed(orderRepository.countConfirmedOrders(kw, pm, os))
                .shipping(orderRepository.countShippingOrders(kw, pm, os))
                .completed(orderRepository.countCompletedOrders(kw, pm, os))
                .cancelled(orderRepository.countCancelledOrders(kw, pm, os))
                .build();
    }

    @Override
    public List<String> getPaymentMethods() {
        return orderRepository.findDistinctPaymentMethods();
    }

    @Override
    public List<String> getOrderStatuses() {
        return orderRepository.findDistinctOrderStatuses();
    }

    @Override
    @Transactional
    public OrderDetailResponse getOrderDetailResponse(String orderCode) {
        Order order = orderRepository.findDetailByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        List<OrderStatusHistory> histories = orderStatusHistoryRepository
                .findByOrder_OrderIdOrderByCreatedAtAsc(order.getOrderId());

        List<OrderStatusHistoryResponse> historyResponses = histories.stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .status(history.getStatus())
                        .title(history.getTitle())
                        .note(history.getNote())
                        .createdAt(history.getCreatedAt())
                        .createdByName(getUserDisplayName(history.getCreatedBy()))
                        .build())
                .toList();

        return OrderDetailResponse.builder()
                .orderCode(order.getOrderCode())
                .orderStatus(order.getOrderStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .shippingAddress(order.getShippingAddress())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .transactionId(order.getTransactionId())
                .paymentDate(order.getPaymentDate())
                .trackingCode(order.getTrackingCode())
                .userId(order.getUser().getUserId())
                .fullName(order.getUser().getFullName())
                .email(order.getUser().getEmail())
                .phoneNumber(order.getUser().getPhoneNumber())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getVoucherCode() : null)
                .voucherDiscountType(order.getVoucher() != null ? order.getVoucher().getDiscountType() : null)
                .voucherDiscountValue(order.getVoucher() != null ? order.getVoucher().getDiscountValue() : null)
                .voucherMaxDiscount(order.getVoucher() != null ? order.getVoucher().getMaxDiscountAmount() : null)
                .items(
                        order.getOrderItems().stream().map(orderItem -> {
                            ProductSku sku = orderItem.getSku();
                            Product product = sku.getProduct();
                            return OrderItemDetailResponse.builder()
                                    .skuId(sku.getSkuId())
                                    .skuCode(sku.getSkuCode())
                                    .productId(product.getProductId())
                                    .productName(product.getProductName())
                                    .productImage(
                                            sku.getSkuImageUrl() != null
                                                    ? sku.getSkuImageUrl()
                                                    : product.getThumbnailUrl())
                                    .quantity(orderItem.getQuantity())
                                    .priceAtPurchase(orderItem.getPriceAtPurchase())
                                    .lineTotal(
                                            orderItem.getPriceAtPurchase()
                                                    .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                                    .imeis(
                                            orderItem.getProductItems().stream()
                                                    .map(item -> ImeiResponse.builder()
                                                            .imeiCode(item.getImeiCode())
                                                            .build())
                                                    .toList())
                                    .build();
                        }).toList())
                .history(historyResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrder() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public OrderDetailResponse getUserOrderDetail(String orderCode) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrderDetailResponse response = getOrderDetailResponse(orderCode);
        if (!userId.equals(response.getUserId())) {
            throw new RuntimeException("Không có quyền xem");
        }
        return response;
    }
}