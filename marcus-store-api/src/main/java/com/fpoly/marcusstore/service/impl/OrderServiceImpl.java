package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        return paymentMethod == null ||
                paymentMethod.isBlank() ||
                "ALL".equalsIgnoreCase(paymentMethod)
                ? null
                : paymentMethod.trim();
    }

    private String normalizeOrderStatus(String orderStatus) {
        return orderStatus == null ||
                orderStatus.isBlank() ||
                "ALL".equalsIgnoreCase(orderStatus)
                ? null
                : orderStatus.trim();
    }

    private String normalizeStatusValue(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }

    private String getUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
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

    @Override
    public Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus, Pageable pageable) {
        String normalizeKeyword = normalizeKeyword(keyword);
        String normalizePaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizeOrderStatus = normalizeOrderStatus(orderStatus);

        return orderRepository
                .searchOrders(normalizeKeyword, normalizePaymentMethod, normalizeOrderStatus, pageable)
                .map(this::toResponse);
    }

    // hàm gọi tổng đơn hàng và số lượng trạng thái đơn hàng
    @Override
    public OrderStatsResponse getOrderStats(String keyword, String paymentMethod, String orderStatus) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedOrderStatus = normalizeOrderStatus(orderStatus);

        return OrderStatsResponse.builder()
                .total(orderRepository.countOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .pending(orderRepository.countPendingOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .confirmed(orderRepository.countConfirmedOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .shipping(orderRepository.countShippingOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .completed(orderRepository.countCompletedOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
                .cancelled(orderRepository.countCancelledOrders(
                        normalizedKeyword,
                        normalizedPaymentMethod,
                        normalizedOrderStatus
                ))
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
        // Lấy chi tiết đơn hàng theo mã đơn hàng
        Order order = orderRepository.findDetailByOrderCode(orderCode).orElseThrow(() ->
                new RuntimeException("không tìm thấy đơn hàng "));
        if (ensureTrackingCodeForShipping(order)) {
            orderRepository.saveAndFlush(order);
        }
        orderItemRepository.findWithProductItemsByOrderId(order.getOrderId());
        // lấy trạng thái lịch sử đơn hàng
        List<OrderStatusHistory> histories =
                orderStatusHistoryRepository.findByOrder_OrderIdOrderByCreatedAtAsc(order.getOrderId());
        // map từ Entity sang DTO
        // mỗi dòng khi được tách ra sẽ là một trạng thái của đơn hàng khi hiển thị lên FE
        List<OrderStatusHistoryResponse> historyResponses = histories.stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .status(history.getStatus())
                        .title(history.getTitle())
                        .note(history.getNote())
                        .createdAt(history.getCreatedAt())
                        .createdByName(getUserDisplayName(history.getCreatedBy()))

                        .build()
                )
                .toList();
        // map từ Entity sang DTO
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
                .trackingCode(order.getTrackingCode())
                .fullName(order.getUser().getFullName())
                .email(order.getUser().getEmail())
                .phoneNumber(order.getUser().getPhoneNumber())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getVoucherCode() : null)
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
                                                            : product.getThumbnailUrl()
                                            )
                                            .quantity(orderItem.getQuantity())
                                            .priceAtPurchase(orderItem.getPriceAtPurchase())
                                            .lineTotal(
                                                    orderItem.getPriceAtPurchase()
                                                            .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                                            )
                                            .imeis(
                                                    orderItem.getProductItems().stream()
                                                            .map(item -> ImeiResponse.builder()
                                                                    .imeiCode(item.getImeiCode())

                                                                    .build()
                                                            )
                                                            .toList()
                                            )

                                            .build();
                                })
                                .toList()
                )
                // ghép dto orderDetail và dto History
                .history(historyResponses)
                .build();


    }
    // logic đổi trạng thái đơn hàng
    // Những trạng thái có thể đổi
    private boolean canChangeStatus(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        currentStatus = normalizeStatusValue(currentStatus);
        newStatus = normalizeStatusValue(newStatus);

        return switch (currentStatus) {
            case "PENDING" -> newStatus.equals("PROCESSING") || newStatus.equals("CONFIRMED") || newStatus.equals("CANCELLED");
            case "PROCESSING" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
            case "CONFIRMED" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
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
            case "PENDING" -> "Khách hàng tạo đơn";
            case "PROCESSING" -> "Đơn đang được xử lý";
            case "CONFIRMED" -> "Nhân viên xác nhận đơn";
            case "SHIPPING" -> "Đơn chuyển sang đang giao hàng";
            case "COMPLETED" -> "Đơn giao thành công";
            case "FAILED" -> "Giao hàng thất bại";
            case "CANCELLED" -> "Đơn đã hủy";
            default -> "Cập nhật trạng thái đơn hàng";
        };
    }

    private String generateTrackingCode(Order order) {
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
        return "GHN" + randomPart;
    }

    private boolean ensureTrackingCodeForShipping(Order order) {
        boolean isShipping = "SHIPPING".equals(normalizeStatusValue(order.getOrderStatus()));
        boolean missingTrackingCode = order.getTrackingCode() == null || order.getTrackingCode().isBlank();

        if (isShipping && missingTrackingCode) {
            order.setTrackingCode(generateTrackingCode(order));
            return true;
        }

        return false;
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

        if (!canChangeStatus(currentStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        String note = request.getNote();

        if (requiresNote(newStatus) && (note == null || note.isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do cho trạng thái này");
        }

        order.setOrderStatus(newStatus);
        ensureTrackingCodeForShipping(order);
        orderRepository.save(order);

        OrderStatusHistory history = createStatusHistory(order, newStatus, note);
        orderStatusHistoryRepository.save(history);

        return getOrderDetailResponse(orderCode);
    }

    @Override
    @Transactional
    public void hideOrder(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setIsHidden(true);
        orderRepository.save(order);
    }
    // lấy danh sách đơn hàng của user theo ID
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrder() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return orderRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderDetailResponse getUserOrderDetail(String orderCode) {
        Integer userId = SecurityUtils.getCurrentUserId();
        orderRepository.findByOrderCodeAndUserUserId(orderCode, userId).orElseThrow(() ->
                new RuntimeException("không tìm thấy đơn hàng theo yêu cầu"));
        return getOrderDetailResponse(orderCode);
    }
}
