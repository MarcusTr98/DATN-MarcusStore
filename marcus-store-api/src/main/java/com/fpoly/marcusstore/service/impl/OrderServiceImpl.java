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
import com.fpoly.marcusstore.event.OrderConfirmedEvent;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import com.fpoly.marcusstore.repository.statistics.CommentEvaluationRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.EmailService;
import com.fpoly.marcusstore.service.OrderCancellationService;
import com.fpoly.marcusstore.service.OrderPaymentService;
import com.fpoly.marcusstore.service.OrderService;
import com.fpoly.marcusstore.service.OrderShippingService;
import com.fpoly.marcusstore.service.ProductItemService;
import com.fpoly.marcusstore.service.AdminNotificationService;
import com.fpoly.marcusstore.service.UserNotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import com.fpoly.marcusstore.utils.CancellationReasonCatalog;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderItemRepository orderItemRepository;
    private final ProductItemRepository productItemRepository;
    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductSkuRepository productSkuRepository;
    private final ApplicationEventPublisher eventPublisher;
    // Marcus thêm: Admin có thể retry GHN sau khi lần tạo vận đơn tự động thất bại.
    private final OrderShippingService orderShippingService;
    private final OrderPaymentService orderPaymentService;
    private final OrderCancellationService orderCancellationService;
    private final EmailService emailService;
    private final CommentEvaluationRepository commentEvaluationRepository;
    // Marcus thêm chuông hai chiều cho luồng hủy đơn.
    private final AdminNotificationService adminNotificationService;
    private final UserNotificationService userNotificationService;

    @Value("${vnpay.paymentTimeoutMinutes:20}")
    private long vnPayPaymentTimeoutMinutes;
    // Marcus sửa: khách được hủy trước khi tạo vận đơn. PACKED đã có tracking GHN
    // nên không thể chỉ hủy nội bộ rồi để vận đơn tiếp tục giao.
    private static final Set<String> USER_CANCELLABLE_STATUSES = Set.of(
            "PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_PICKUP");

    // Marcus thêm: IMEI chỉ gồm chữ số, độ dài 8-20. Validate chặt để FE gõ nhầm
    // IMEI ngắn/dài/ký tự đặc biệt sẽ bị BE chặn ngay tại đầu vào.
    private static final java.util.regex.Pattern IMEI_PATTERN = java.util.regex.Pattern.compile("^[0-9]{8,20}$");

    // Marcus thêm: retry GHN độc lập, không làm mất bước kiểm soát IMEI của module kho.
    @Override
    public OrderDetailResponse retryGhnShipment(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        orderShippingService.createOrRetryGhnOrder(order.getOrderId());
        return getOrderDetailResponse(orderCode);
    }

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
        return updateStatusOrder(orderCode, request, "ADMIN");
    }

    private OrderDetailResponse updateStatusOrder(
            String orderCode, UpdateOrderStatusRequest request, String cancellationActor) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
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
            // Marcus sửa: PACKED là nghiệp vụ của đơn và phải commit độc lập với
            // HTTP GHN. Listener sau-commit sẽ tạo vận đơn và lưu FAILED nếu lỗi.
            order.setGhnIntegrationStatus("PENDING");
            order.setGhnLastError(null);
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
            // Hoàn kho, voucher, giỏ hàng và số lượng Flash Sale tại một nơi
            orderCancellationService.cancelAndRestore(
                    order, request.getCancellationReasonCode(), cancellationActor, note);
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

        // Marcus thêm: mọi bước Admin cập nhật trong vòng đời đơn đều phát chuông
        // cho đúng khách hàng. Service tự chuẩn hóa title/type theo trạng thái.
        userNotificationService.createOrderStatusNotification(
                order,
                newStatus,
                "CANCELLED".equals(newStatus)
                        ? "Đơn " + order.getOrderCode() + " đã hủy. Lý do: " + note
                        : null);

        if (isPackingNow) {
            eventPublisher.publishEvent(new OrderConfirmedEvent(this, order.getOrderId()));
        }

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
                .cancellationReasonCode(order.getCancellationReasonCode())
                .cancellationReasonLabel(order.getCancellationReasonCode() == null
                        ? null : CancellationReasonCatalog.label(order.getCancellationReasonCode()))
                .cancellationActor(order.getCancellationActor())
                .cancelledAt(order.getCancelledAt())
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
                .paymentExpiresAt(resolvePaymentExpiresAt(order))
                .trackingCode(order.getTrackingCode())
                .ghnIntegrationStatus(order.getGhnIntegrationStatus())
                .ghnRetryCount(order.getGhnRetryCount())
                .ghnLastError(order.getGhnLastError())
                .ghnLastAttemptAt(order.getGhnLastAttemptAt())
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
    public OrderDetailResponse cancelUserOrder(String orderCode, String reasonCode, String reason) {
        Integer userId = SecurityUtils.getCurrentUserId();

        // Marcus sửa: khóa dòng Order trước khi kiểm tra và hoàn tài nguyên. Nhờ đó
        // retry của khách không thể đua với IPN VNPAY, scheduler hoặc Admin hủy đơn.
        Order order = orderRepository.findByOrderCodeAndUserIdForUpdate(orderCode, userId)
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
                .cancellationReasonCode(reasonCode)
                .build();
        OrderDetailResponse response = updateStatusOrder(orderCode, request, "CUSTOMER");
        // Marcus thêm: khi khách tự hủy, chủ cửa hàng nhận chuông realtime để nắm
        // lý do và dừng xử lý đơn.
        adminNotificationService.createAndSendNotification(
                "ORDER_CANCELLED",
                "Khách đã hủy đơn " + order.getOrderCode(),
                "Khách hàng " + getUserDisplayName(order.getUser()) + " hủy đơn. Lý do: " + request.getNote(),
                order.getOrderCode());
        return response;
    }

    private LocalDateTime resolvePaymentExpiresAt(Order order) {
        if (!"VNPAY".equalsIgnoreCase(order.getPaymentMethod())
                || order.getCreatedAt() == null
                || !"PENDING".equalsIgnoreCase(order.getPaymentStatus())) {
            return null;
        }
        return order.getCreatedAt().plusMinutes(vnPayPaymentTimeoutMinutes);
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

    // Đức thêm xử lý imei cho order
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
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailResponse assignOrderImeis(String orderCode, List<UpdateOrderImeiRequest> requests) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Marcus sửa: chỉ cho phép gán IMEI khi đơn đang ở PROCESSING hoặc
        // READY_FOR_PICKUP.
        // - PROCESSING: flow COD (PROCESSING → PACKED) hoặc STORE_PICKUP (PROCESSING →
        // READY_FOR_PICKUP).
        // - READY_FOR_PICKUP: trường hợp admin đã chuyển READY_FOR_PICKUP trước đó
        // nhưng sau
        // đó muốn bổ sung IMEI cho dòng chưa có IMEI (fix gán bổ sung sau
        // auto-transition).
        // Tránh gán IMEI trên đơn đã SHIPPING/DELIVERED/COMPLETED — lúc đó IMEI thực tế
        // đang
        // ở kho khách, việc đụng vào IMEI là sai nghiệp vụ.
        String currentStatus = normalizeStatusValue(order.getOrderStatus());
        if (!"PROCESSING".equals(currentStatus) && !"READY_FOR_PICKUP".equals(currentStatus)) {
            throw new RuntimeException(
                    "Chỉ có thể gán IMEI khi đơn đang ở trạng thái Đang chuẩn bị hoặc Sẵn sàng nhận tại cửa hàng");
        }

        if (requests == null || requests.isEmpty()) {
            // Không có dòng nào cần gán → bỏ qua, đơn vẫn ở trạng thái hiện tại.
            return getOrderDetailResponse(orderCode);
        }

        // Marcus gom tất cả IMEI của toàn bộ request để check trùng giữa các dòng đơn
        // (Bug 3: admin nhập nhầm cùng IMEI cho 2 dòng khác nhau).
        Map<Integer, List<String>> imeisByOrderItem = new java.util.LinkedHashMap<>();
        for (UpdateOrderImeiRequest req : requests) {
            OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy dòng đơn hàng: " + req.getOrderItemId()));

            if (!orderItem.getOrder().getOrderId().equals(order.getOrderId())) {
                throw new RuntimeException("Dòng đơn hàng không thuộc đơn này");
            }

            ProductSku sku = orderItem.getSku();
            boolean hasImei = sku.getProduct() != null
                    && Boolean.TRUE.equals(sku.getProduct().getStatusImei());
            if (!hasImei) {
                throw new RuntimeException("SKU " + sku.getSkuCode() + " không phải sản phẩm có IMEI");
            }

            // Marcus sửa: cho phép gán bổ sung — chỉ validate số lượng mới nhập, không
            // yêu cầu bằng tổng quantity. Admin có thể gán dần qua nhiều lần.
            long alreadyAssigned = productItemRepository.countByOrderItemId(orderItem.getOrderItemId());
            int remaining = orderItem.getQuantity() - (int) alreadyAssigned;
            if (remaining <= 0) {
                throw new RuntimeException("SKU " + sku.getSkuCode() + " đã gán đủ IMEI");
            }

            List<String> cleaned = req.getImeiCodes() == null ? java.util.Collections.emptyList()
                    : req.getImeiCodes().stream()
                            .map(s -> s == null ? "" : s.trim())
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .toList();

            for (String code : cleaned) {
                if (!IMEI_PATTERN.matcher(code).matches()) {
                    throw new RuntimeException(
                            "IMEI '" + code + "' không hợp lệ (chỉ chấp nhận chữ số, 8-20 ký tự)");
                }
            }

            if (cleaned.size() > remaining) {
                throw new RuntimeException("SKU " + sku.getSkuCode() + " chỉ còn "
                        + remaining + " IMEI cần gán nhưng nhập " + cleaned.size() + " mã");
            }

            imeisByOrderItem.put(orderItem.getOrderItemId(), cleaned);
        }

        List<String> allImeis = imeisByOrderItem.values().stream()
                .flatMap(List::stream)
                .toList();
        Set<String> uniqueImeis = new java.util.HashSet<>(allImeis);
        if (uniqueImeis.size() != allImeis.size()) {
            Set<String> seen = new java.util.HashSet<>();
            Set<String> duplicates = new java.util.LinkedHashSet<>();
            for (String code : allImeis) {
                if (!seen.add(code)) {
                    duplicates.add(code);
                }
            }
            throw new RuntimeException(
                    "IMEI bị trùng giữa các dòng đơn: " + String.join(", ", duplicates));
        }

        Set<Integer> skuIds = requests.stream()
                .map(req -> orderItemRepository.findById(req.getOrderItemId())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy dòng đơn hàng: " + req.getOrderItemId()))
                        .getSku().getSkuId())
                .collect(Collectors.toSet());

        Map<Integer, ProductSku> lockedSkus = skuIds.isEmpty()
                ? Map.of()
                : productSkuRepository.findByIdsForUpdate(new java.util.ArrayList<>(skuIds)).stream()
                        .collect(Collectors.toMap(ProductSku::getSkuId, s -> s));

        List<ProductItem> lockedImeis = productItemRepository
                .findAvailableByImeiCodesForUpdate(allImeis);
        Map<String, ProductItem> imeiMap = lockedImeis.stream()
                .collect(Collectors.toMap(ProductItem::getImeiCode, pi -> pi));

        Set<String> missing = new java.util.LinkedHashSet<>();
        for (String code : allImeis) {
            if (!imeiMap.containsKey(code)) {
                missing.add(code);
            }
        }
        if (!missing.isEmpty()) {
            throw new RuntimeException(
                    "IMEI không tồn tại hoặc không khả dụng: " + String.join(", ", missing));
        }

        for (Map.Entry<Integer, List<String>> entry : imeisByOrderItem.entrySet()) {
            Integer orderItemId = entry.getKey();
            OrderItem orderItem = orderItemRepository.findById(orderItemId).orElseThrow();
            Integer skuId = orderItem.getSku().getSkuId();
            for (String code : entry.getValue()) {
                ProductItem pi = imeiMap.get(code);
                if (!pi.getProductSku().getSkuId().equals(skuId)) {
                    throw new RuntimeException(
                            "IMEI " + code + " không thuộc SKU " + orderItem.getSku().getSkuCode());
                }
            }
        }

        for (UpdateOrderImeiRequest req : requests) {
            OrderItem orderItem = orderItemRepository.findById(req.getOrderItemId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy dòng đơn hàng: " + req.getOrderItemId()));
            List<String> codes = imeisByOrderItem.get(orderItem.getOrderItemId());

            for (String code : codes) {
                ProductItem pi = imeiMap.get(code);
                pi.setOrderItem(orderItem);
                pi.setStatus(ProductItemService.STATUS_SOLD);
                productItemRepository.save(pi);
            }
        }

        order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderId(order.getOrderId());

        boolean allItemsFullyAssigned = orderItems.stream().allMatch(orderItem -> {
            ProductSku sku = orderItem.getSku();
            boolean hasImei = sku.getProduct() != null
                    && Boolean.TRUE.equals(sku.getProduct().getStatusImei());
            if (!hasImei)
                return true;
            long assignedCount = productItemRepository.countByOrderItemId(orderItem.getOrderItemId());
            return assignedCount >= orderItem.getQuantity();
        });

        if (allItemsFullyAssigned) {
            String status = normalizeStatusValue(order.getOrderStatus());
            boolean storePickup = "STORE_PICKUP".equalsIgnoreCase(order.getFulfillmentMethod());
            String nextStatus = null;
            String note = null;
            if ("PROCESSING".equals(status)) {
                nextStatus = storePickup ? "READY_FOR_PICKUP" : "PACKED";
                note = storePickup
                        ? "Auto-transition: gán đủ IMEI cho đơn nhận tại cửa hàng"
                        : "Auto-transition: gán đủ IMEI cho tất cả dòng đơn";
            }

            if (nextStatus != null) {
                order.setOrderStatus(nextStatus);
                orderRepository.save(order);

                OrderStatusHistory history = createStatusHistory(order, nextStatus, note);
                orderStatusHistoryRepository.save(history);

                if ("PACKED".equals(nextStatus) && !storePickup) {
                    try {
                        orderShippingService.processCreateGhnOrder(order);
                    } catch (Exception e) {
                        System.err.println("Auto-create GHN order failed for "
                                + order.getOrderCode() + ": " + e.getMessage());
                    }
                }
            }
        }

        return getOrderDetailResponse(orderCode);
    }

    // Đức thêm: gộp transition PROCESSING + gán IMEI làm 1 transaction duy nhất.
    // Nếu IMEI không hợp lệ/không đủ số lượng → toàn bộ rollback, đơn vẫn ở
    // CONFIRMED,
    // tránh trường hợp đơn bị treo ở PROCESSING mà chưa có IMEI nào.
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailResponse startProcessingWithImei(String orderCode, List<UpdateOrderImeiRequest> requests) {
        Order order = orderRepository.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        String currentStatus = normalizeStatusValue(order.getOrderStatus());

        if (!canChangeStatus(order, currentStatus, "PROCESSING")) {
            throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang PROCESSING");
        }

        // Bước 1: chuyển sang PROCESSING trước (trong cùng transaction)
        UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
                .status("PROCESSING")
                .note(null)
                .build();
        updateStatusOrder(orderCode, statusRequest);

        // Bước 2: gán IMEI — lúc này order đã PROCESSING nên pass được validate trong
        // assignOrderImeis()
        // Nếu bước này throw exception (IMEI sai/thiếu), @Transactional sẽ rollback
        // luôn bước 1.
        return assignOrderImeis(orderCode, requests);
    }
}
