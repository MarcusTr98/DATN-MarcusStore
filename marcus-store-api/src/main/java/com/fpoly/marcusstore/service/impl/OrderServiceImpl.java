package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.EmailService;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.OrderPaymentService;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.OrderShippingService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;
    private final OrderShippingService orderShippingService;
    private final OrderPaymentService orderPaymentService;
    private final OrderCancellationService orderCancellationService;
    private final EmailService emailService;


    private static final Set<String> USER_CANCELLABLE_STATUSES = Set.of("PENDING", "PROCESSING", "PACKED");

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
            case "PROCESSING" -> newStatus.equals("PACKED") || newStatus.equals("CANCELLED");
            case "PACKED" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
            case "SHIPPING" -> newStatus.equals("DELIVERED") || newStatus.equals("FAILED");
            case "DELIVERED" -> newStatus.equals("COMPLETED");
            case "FAILED" -> newStatus.equals("SHIPPING") || newStatus.equals("CANCELLED");
            default -> false;
        };
    }

    private boolean requiresNote(String status) {
        return "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    private String getHistoryTitle(String status) {
        return switch (status) {
            case "CREATED" -> "Tạo đơn";
            case "PENDING" -> "Đơn hàng đã đặt";
            case "CONFIRMED" -> "Đơn hàng đã được xác nhận";
            case "PROCESSING" -> "Đơn hàng đang được chuẩn bị";
            case "PACKED" -> "Đơn hàng đã đóng gói";
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "DELIVERED" -> "Giao hàng thành công";
            case "COMPLETED" -> "Đơn hàng hoàn thành";
            case "FAILED" -> "Giao hàng không thành công";
            case "CANCELLED" -> "Đơn hàng đã hủy";
            default -> "Cập nhật trạng thái";
        };
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
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        String currentStatus = normalizeStatusValue(order.getOrderStatus());
        String newStatus = normalizeStatusValue(request.getStatus());

        if (newStatus == null || newStatus.isBlank()) {
            throw new RuntimeException("Trạng thái mới không hợp lệ");
        }

        if (!canChangeStatus(currentStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        // Tạo vận đơn GHN đúng lúc admin đóng gói đơn hàng
        boolean isPackingNow = "PACKED".equals(newStatus) && !"PACKED".equals(currentStatus);

        if (isPackingNow) {
            try {
                orderShippingService.processCreateGhnOrder(order);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tạo mã vận đơn GHN: " + e.getMessage());
            }
        }

        String note = request.getNote();
        if (requiresNote(newStatus) && (note == null || note.isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do cho trạng thái này");
        }

        if ("CANCELLED".equals(newStatus)) {
            // Hoàn kho, voucher, giỏ hàng và số lượng Flash Sale tại một nơi
            orderCancellationService.cancelAndRestore(order, note);
        } else {
            order.setOrderStatus(newStatus);
        }

        if ("COMPLETED".equals(newStatus)) {
            String transactionType = "COD".equalsIgnoreCase(order.getPaymentMethod())
                    ? "COD_COLLECTION"
                    : "VNPAY_PAYMENT";
            orderPaymentService.handlePaymentSuccess(
                    order,
                    transactionType,
                    "ORDER_COMPLETED:" + order.getOrderCode());
        }
        orderRepository.save(order);

        // Gửi email thông báo trạng thái đơn hàng cho khách
        try {
            emailService.sendOrderStatusUpdate(
                    order.getUser().getEmail(),
                    getUserDisplayName(order.getUser()),
                    order,
                    newStatus);
        } catch (Exception e) {
            // log lỗi, không rollback transaction cập nhật đơn hàng
            // log.error("Gửi mail cập nhật đơn hàng thất bại cho order {}: {}", orderCode,
            // e.getMessage());
        }

        OrderStatusHistory history = createStatusHistory(order, newStatus, note);
        orderStatusHistoryRepository.save(history);

        return getOrderDetailResponse(orderCode);
    }

    @Override
    public Page<OrderResponse> getOrdersPage(String keyword, String paymentMethod, String orderStatus,
                                             LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return orderRepository.searchOrders(
                        normalizeKeyword(keyword), normalizePaymentMethod(paymentMethod), normalizeOrderStatus(orderStatus),
                        fromDate, toDate, pageable)
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

        // Fetch tất cả SKU với variants trong 1 query để tránh N+1
        List<Integer> skuIds = order.getOrderItems().stream()
                .map(item -> item.getSku().getSkuId())
                .toList();
        Map<Integer, ProductSku> skuVariantMap = skuIds.isEmpty()
                ? Map.of()
                : productSkuRepository.findBySkuIdIn(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, sku -> sku));

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
                .shippingSubsidy(order.getShippingSubsidy())
                .deliveryNote(order.getDeliveryNote())
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
                            // Lấy SKU có fetch variants từ map (tránh N+1 và LazyInit)
                            ProductSku skuWithVariants = skuVariantMap.getOrDefault(sku.getSkuId(), sku);
                            List<ClientSkuAttributeValueResponse> variants = skuWithVariants
                                    .getAttributeValues() == null
                                    ? List.of()
                                    : skuWithVariants.getAttributeValues().stream()
                                    .map(av -> ClientSkuAttributeValueResponse.builder()
                                            .valueId(av.getValueId())
                                            .attributeId(av.getAttribute() != null
                                                    ? av.getAttribute().getAttributeId()
                                                    : null)
                                            .attributeName(av.getAttribute() != null
                                                    ? av.getAttribute().getAttributeName()
                                                    : null)
                                            .valueString(av.getValueString())
                                            .valueMeta(av.getValueMeta())
                                            .build())
                                    .toList();
                            return OrderItemDetailResponse.builder()
                                    .skuId(sku.getSkuId())
                                    .skuCode(sku.getSkuCode())
                                    .productId(product.getProductId())
                                    .productName(product.getProductName())
                                    .productImage(product.getThumbnailUrl())
                                    .quantity(orderItem.getQuantity())
                                    .priceAtPurchase(orderItem.getPriceAtPurchase())
                                    .lineTotal(orderItem.getPriceAtPurchase()
                                            .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                                    .isFlashSale(orderItem.getIsFlashSale())
                                    .originalPrice(orderItem.getOriginalPrice())
                                    .flashSaleSlotName(orderItem.getFlashSaleSlotName())
                                    .variants(variants)
                                    .imeis(orderItem.getProductItems().stream()
                                            .map(item -> ImeiResponse.builder().imeiCode(item.getImeiCode()).build())
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

    @Override
    @Transactional
    public OrderDetailResponse cancelUserOrder(String orderCode, String reason) {
        Integer userId = SecurityUtils.getCurrentUserId();

        Order order = orderRepository.findByOrderCodeAndUserUserId(orderCode, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Marcus sửa: cho phép khách hủy cả COD và VNPAY trong các trạng thái an toàn.
        if (!("COD".equalsIgnoreCase(order.getPaymentMethod())
                || "VNPAY".equalsIgnoreCase(order.getPaymentMethod()))) {
            throw new RuntimeException("Phương thức thanh toán này chưa hỗ trợ tự hủy");
        }

        String currentStatus = normalizeStatusValue(order.getOrderStatus());
        if (!USER_CANCELLABLE_STATUSES.contains(currentStatus)) {
            throw new RuntimeException("Không thể hủy đơn ở trạng thái " + currentStatus);
        }

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("CANCELLED")
                .note((reason == null || reason.isBlank()) ? "Khách hàng tự hủy" : reason)
                .build();
        return updateStatusOrder(orderCode, request);
    }
}
