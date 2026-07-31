package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.UpdateOrderImeiRequest;
import com.fpoly.marcusstore.dto.request.UpdateOrderStatusRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductItem;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.OrderPaymentService;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.OrderShippingService;
import com.fpoly.marcusstore.service.AdminNotificationService;
import com.fpoly.marcusstore.service.EmailService;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.UserNotificationService;
import com.fpoly.marcusstore.service.ProductItemService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ProductItemRepository productItemRepository;
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final OrderShippingService orderShippingService;
    private final OrderPaymentService orderPaymentService;
    private final OrderCancellationService orderCancellationService;
    private final EmailService emailService;
    private final CommentEvaluationRepository commentEvaluationRepository;
    // Marcus thêm chuông hai chiều cho luồng hủy đơn.
    private final AdminNotificationService adminNotificationService;
    private final UserNotificationService userNotificationService;
    // Marcus sửa: khách được hủy trước khi tạo vận đơn. PACKED đã có tracking GHN
    // nên không thể chỉ hủy nội bộ rồi để vận đơn tiếp tục giao.
    private static final Set<String> USER_CANCELLABLE_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_PICKUP");

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
                .fulfillmentMethod(order.getFulfillmentMethod())
                .createdAt(order.getCreatedAt()).build();
    }

    private boolean canChangeStatus(Order order, String currentStatus, String newStatus) {
        boolean storePickup = "STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod());
        return switch (currentStatus) {
            case "PENDING" -> newStatus.equals("CONFIRMED") || newStatus.equals("CANCELLED");
            case "CONFIRMED" -> newStatus.equals("PROCESSING") || newStatus.equals("CANCELLED");
            case "PROCESSING" -> storePickup
                    ? newStatus.equals("READY_FOR_PICKUP") || newStatus.equals("CANCELLED")
                    : newStatus.equals("PACKED") || newStatus.equals("CANCELLED");
            case "READY_FOR_PICKUP" -> newStatus.equals("COMPLETED") || newStatus.equals("CANCELLED");
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
            case "READY_FOR_PICKUP" -> "Đơn hàng sẵn sàng nhận tại cửa hàng";
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "DELIVERED" -> "Giao hàng thành công";
            case "COMPLETED" -> "Đơn hàng hoàn thành";
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
        String newStatus = normalizeStatusValue(request.getStatus());

        if (newStatus == null || newStatus.isBlank()) {
            throw new RuntimeException("Trạng thái mới không hợp lệ");
        }

        // Marcus thêm: admin chỉ được hủy, không được xác nhận/chuẩn bị đơn VNPAY chưa
        // thu tiền.
        if (isAwaitingVnPayPayment(order) && !"CANCELLED".equals(newStatus)) {
            throw new RuntimeException(
                    "Đơn VNPAY chưa thanh toán; không thể xác nhận hoặc chuẩn bị hàng");
        }

        if (!canChangeStatus(order, currentStatus, newStatus)) {
            throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        // Tạo vận đơn GHN đúng lúc admin đóng gói đơn hàng
        boolean isPackingNow = "PACKED".equals(newStatus) && !"PACKED".equals(currentStatus)
                && !"STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod());

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
            // Marcus thêm: không được hủy cục bộ khi vận đơn GHN đã tồn tại, nếu
            // không shipper vẫn giao dù kho/voucher đã được hoàn.
            if (!"STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod())
                    && order.getTrackingCode() != null
                    && !order.getTrackingCode().isBlank()) {
                throw new RuntimeException(
                        "Đơn đã có vận đơn GHN; cần hủy vận đơn GHN trước khi hủy đơn trên hệ thống");
            }
            // Hoàn kho (theo số lượng), voucher, giỏ hàng, Flash Sale tại một nơi
            orderCancellationService.cancelAndRestore(order, note);

            // Hoàn trạng thái IMEI đã gán về IN_STOCK — chỉ đổi trạng thái, KHÔNG cộng lại
            // stockQuantity vì cancelAndRestore ở trên đã cộng theo quantity rồi (tránh
            // cộng trùng)
            List<OrderItem> orderItemsForImei = orderItemRepository.findByOrder_OrderId(order.getOrderId());
            for (OrderItem item : orderItemsForImei) {
                List<ProductItem> assignedImeis = item.getProductItems();
                if (assignedImeis != null && !assignedImeis.isEmpty()) {
                    for (ProductItem pi : assignedImeis) {
                        pi.setStatus(ProductItemService.STATUS_IN_STOCK);
                        pi.setOrderItem(null);
                        productItemRepository.save(pi);
                    }
                }
            }
        } else {
            order.setOrderStatus(newStatus);
        }

        markPaymentPaidWhenCompleted(order);
        orderRepository.save(order);

        OrderStatusHistory history = createStatusHistory(order, newStatus, note);
        orderStatusHistoryRepository.save(history);

        // Marcus thêm: mọi bước Admin cập nhật trong vòng đời đơn đều phát chuông
        // cho đúng khách hàng. Service tự chuẩn hóa title/type theo trạng thái.
        userNotificationService.createOrderStatusNotification(
                order,
                newStatus,
                "CANCELLED".equals(newStatus)
                        ? "Đơn " + order.getOrderCode() + " đã hủy. Lý do: " + note
                        : null);

        return getOrderDetailResponse(orderCode);
    }

    private boolean isAwaitingVnPayPayment(Order order) {
        return "VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                && !"PAID".equalsIgnoreCase(order.getPaymentStatus());
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
                .fulfillmentMethod(order.getFulfillmentMethod())
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
                            boolean reviewed = commentEvaluationRepository.existsByOrderItemOrderItemId(
                                    orderItem.getOrderItemId());
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
                                    .orderItemId(orderItem.getOrderItemId())
                                    .skuId(sku.getSkuId())
                                    .skuCode(sku.getSkuCode())
                                    .productId(product.getProductId())
                                    .productSlug(product.getSlug()) // Marcus thêm productSlug vào response
                                    .productName(product.getProductName())
                                    .productImage(sku.getSkuImageUrl() != null ? sku.getSkuImageUrl()
                                            : product.getThumbnailUrl())
                                    .quantity(orderItem.getQuantity())
                                    .priceAtPurchase(orderItem.getPriceAtPurchase())
                                    .lineTotal(orderItem.getPriceAtPurchase()
                                            .multiply(BigDecimal.valueOf(orderItem.getQuantity())))
                                    .imeis(orderItem.getProductItems().stream()
                                            .map(item -> ImeiResponse.builder().imeiCode(item.getImeiCode()).build())
                                            .toList())
                                    .reviewed(reviewed)
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

        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Chỉ hỗ trợ hủy đơn COD");
        }

        String currentStatus = normalizeStatusValue(order.getOrderStatus());
        if (!USER_CANCELLABLE_STATUSES.contains(currentStatus)) {
            throw new RuntimeException("Không thể hủy đơn ở trạng thái " + currentStatus);
        }

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("CANCELLED")
                .note((reason == null || reason.isBlank()) ? "Khách hàng tự hủy" : reason)
                .build();
        OrderDetailResponse response = updateStatusOrder(orderCode, request);
        // Marcus thêm: khi khách tự hủy, chủ cửa hàng nhận chuông realtime để nắm
        // lý do và dừng xử lý đơn.
        adminNotificationService.createAndSendNotification(
                "ORDER_CANCELLED",
                "Khách đã hủy đơn " + order.getOrderCode(),
                "Khách hàng " + getUserDisplayName(order.getUser()) + " hủy đơn. Lý do: " + request.getNote(),
                order.getOrderCode());
        return response;
    }

    @Override
    @Transactional
    public OrderDetailResponse confirmUserReceivedOrder(String orderCode) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getUser() == null || !userId.equals(order.getUser().getUserId())) {
            throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
        }

        String currentStatus = normalizeStatusValue(order.getOrderStatus());
        // Marcus thêm idempotency: retry do mạng chậm không tạo lịch sử, thông báo
        // hay giao dịch thanh toán lần thứ hai.
        if ("COMPLETED".equals(currentStatus)) {
            return getOrderDetailResponse(orderCode);
        }

        boolean storePickup = "STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod());
        String requiredStatus = storePickup ? "READY_FOR_PICKUP" : "DELIVERED";
        if (!requiredStatus.equals(currentStatus)) {
            throw new RuntimeException(storePickup
                    ? "Chỉ xác nhận khi đơn đã sẵn sàng nhận tại cửa hàng"
                    : "Chỉ xác nhận khi đơn vị vận chuyển đã giao hàng thành công");
        }

        if ("VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new RuntimeException("Đơn VNPAY chưa được xác nhận thanh toán");
        }

        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status("COMPLETED")
                .note(storePickup
                        ? "Khách hàng xác nhận đã nhận hàng tại cửa hàng"
                        : "Khách hàng xác nhận đã nhận hàng từ đơn vị vận chuyển")
                .build();
        OrderDetailResponse response = updateStatusOrder(orderCode, request);

        adminNotificationService.createAndSendNotification(
                "ORDER_COMPLETED",
                "Khách xác nhận đã nhận đơn " + order.getOrderCode(),
                storePickup
                        ? "Khách hàng đã xác nhận nhận hàng tại Marcus Store."
                        : "Khách hàng đã xác nhận nhận hàng từ đơn vị vận chuyển.",
                order.getOrderCode());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderImeiAssignmentResponse> getImeiPreview(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());

        return orderItems.stream().map(item -> {
            ProductSku sku = item.getSku();
            boolean hasImei = sku.getProduct() != null && Boolean.TRUE.equals(sku.getProduct().getStatusImei());

            List<OrderImeiAssignmentResponse.ImeiDetailItem> available = java.util.Collections.emptyList();
            List<String> assignedImeis = item.getProductItems().stream()
                    .map(ProductItem::getImeiCode)
                    .toList();
            if (hasImei) {
                List<ProductItem> availableItems = productItemRepository.findAvailableBySkuId(sku.getSkuId());
                available = availableItems.stream()
                        .map(pi -> OrderImeiAssignmentResponse.ImeiDetailItem.builder()
                                .itemId(pi.getItemId())
                                .imeiCode(pi.getImeiCode())
                                .status(pi.getStatus())
                                .statusLabel(ProductItemService.toStatusLabel(pi.getStatus()))
                                .createdAt(pi.getCreatedAt())
                                .build())
                        .toList();
            }

            return OrderImeiAssignmentResponse.builder()
                    .orderItemId(item.getOrderItemId())
                    .skuCode(sku.getSkuCode())
                    .productName(sku.getProduct() != null ? sku.getProduct().getProductName() : "")
                    .quantityOrdered(item.getQuantity())
                    .quantityAssigned(item.getProductItems().size())
                    .assignedImeis(assignedImeis)
                    .availableImeis(available)
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public OrderDetailResponse assignOrderImeis(String orderCode, List<UpdateOrderImeiRequest> requests) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        for (UpdateOrderImeiRequest req : requests) {
            OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dòng đơn hàng: " + req.getOrderItemId()));

            if (!orderItem.getOrder().getOrderId().equals(order.getOrderId())) {
                throw new RuntimeException("Dòng đơn hàng không thuộc đơn này");
            }

            ProductSku sku = orderItem.getSku();
            boolean hasImei = sku.getProduct() != null && Boolean.TRUE.equals(sku.getProduct().getStatusImei());
            if (!hasImei) {
                throw new RuntimeException("SKU " + sku.getSkuCode() + " không phải sản phẩm có IMEI");
            }

            List<String> imeis = req.getImeiCodes().stream()
                    .map(String::trim)
                    .filter(s -> s != null && !s.isEmpty())
                    .distinct()
                    .toList();

            if (imeis.size() != orderItem.getQuantity()) {
                throw new RuntimeException("SKU " + sku.getSkuCode() + " cần " + orderItem.getQuantity()
                        + " IMEI nhưng nhập " + imeis.size() + " mã");
            }

            List<ProductItem> availableBySku = productItemRepository.findAvailableBySkuId(sku.getSkuId());
            Map<String, ProductItem> availableMap = availableBySku.stream()
                    .collect(Collectors.toMap(ProductItem::getImeiCode, pi -> pi));

            List<String> notFound = new java.util.ArrayList<>();
            List<ProductItem> toAssign = new java.util.ArrayList<>();
            for (String imei : imeis) {
                ProductItem pi = availableMap.get(imei);
                if (pi == null) {
                    // Thử tìm theo mã IMEI trực tiếp (IMEI nhập tay / chưa có trong kho)
                    pi = productItemRepository.findAvailableByImeiCode(imei);
                    if (pi == null) {
                        notFound.add(imei);
                        continue;
                    }
                    // Verify đúng SKU
                    if (!pi.getProductSku().getSkuId().equals(sku.getSkuId())) {
                        throw new RuntimeException("IMEI " + imei + " không thuộc SKU " + sku.getSkuCode());
                    }
                }
                toAssign.add(pi);
            }

            if (!notFound.isEmpty()) {
                throw new RuntimeException("IMEI không tồn tại hoặc không khả dụng: " + String.join(", ", notFound));
            }

            for (ProductItem pi : toAssign) {
                pi.setOrderItem(orderItem);
                pi.setStatus(ProductItemService.STATUS_SOLD);
                productItemRepository.save(pi);

                // Sync sku.stockQuantity giảm xuống (IMEI chuyển từ IN_STOCK → SOLD)
                Integer skuId = pi.getProductSku().getSkuId();
                ProductSku piSku = productSkuRepository.findById(skuId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU: " + skuId));
                if (piSku.getStockQuantity() == null || piSku.getStockQuantity() <= 0) {
                    throw new RuntimeException(
                            "SKU " + piSku.getSkuCode() + " đã hết tồn kho, không thể gán IMEI");
                }
                piSku.setStockQuantity(piSku.getStockQuantity() - 1);
                productSkuRepository.save(piSku);
            }
        }

        // Reload order sau khi gán IMEI với orderItems fresh
        order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());

        // Nếu tất cả OrderItem đã gán đủ IMEI (nếu SKU có IMEI) thì auto chuyển sang
        // PACKED
        boolean allItemsFullyAssigned = orderItems.stream().allMatch(orderItem -> {
            ProductSku sku = orderItem.getSku();
            boolean hasImei = sku.getProduct() != null && Boolean.TRUE.equals(sku.getProduct().getStatusImei());
            if (!hasImei)
                return true;
            // Dùng query count trực tiếp từ DB để tránh lazy collection cache cũ
            long assignedCount = productItemRepository.countByOrderItemId(orderItem.getOrderItemId());
            return assignedCount >= orderItem.getQuantity();
        });

        // Auto transition: PROCESSING/READY_TO_PREPARE/CONFIRMED/READY_TO_SHIP ->
        // PACKED
        if (allItemsFullyAssigned) {
            String currentStatus = normalizeStatusValue(order.getOrderStatus());
            if ("PROCESSING".equals(currentStatus) || "READY_TO_PREPARE".equals(currentStatus)
                    || "CONFIRMED".equals(currentStatus) || "READY_TO_SHIP".equals(currentStatus)) {
                order.setOrderStatus("PACKED");
                orderRepository.save(order);

                OrderStatusHistory history = createStatusHistory(
                        order,
                        "PACKED",
                        "Auto-transition: gán đủ IMEI cho tất cả dòng đơn");
                orderStatusHistoryRepository.save(history);
            }
        }

        return getOrderDetailResponse(orderCode);
    }
}