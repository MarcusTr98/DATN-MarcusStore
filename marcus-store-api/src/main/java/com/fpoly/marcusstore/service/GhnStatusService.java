package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.repository.shopping.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnStatusService {

    public enum SyncResult {
        UPDATED, NO_CHANGE, INVALID_TRANSITION, UNSUPPORTED_STATUS, ORDER_NOT_FOUND
    }

    private static final Map<String, String> GHN_STATUS_MAP = Map.ofEntries(
            Map.entry("picking", "SHIPPING"),
            Map.entry("picked", "SHIPPING"),
            Map.entry("storing", "SHIPPING"),
            Map.entry("transporting", "SHIPPING"),
            Map.entry("sorting", "SHIPPING"),
            Map.entry("delivering", "SHIPPING"),
            Map.entry("money_collect_delivering", "SHIPPING"),
            Map.entry("delivered", "DELIVERED"),

            // Các trạng thái này chưa kết thúc vòng đời vận chuyển: đơn vẫn có thể
            // được giao lại hoặc hàng vẫn đang trên đường hoàn về shop.
            Map.entry("delivery_fail", "FAILED"),
            Map.entry("waiting_to_return", "FAILED"),
            Map.entry("return", "FAILED"),
            Map.entry("return_transporting", "FAILED"),
            Map.entry("return_sorting", "FAILED"),
            Map.entry("returning", "FAILED"),
            Map.entry("return_fail", "FAILED"),
            Map.entry("exception", "FAILED"),
            Map.entry("damage", "FAILED"),
            Map.entry("lost", "FAILED"),

            // Chỉ hủy và mở yêu cầu hoàn tiền khi GHN xác nhận vận đơn đã hủy
            // hoặc hàng đã thực sự được trả về người gửi.
            Map.entry("cancel", "CANCELLED"),
            Map.entry("returned", "CANCELLED"));

    private static final Map<String, Set<String>> ALLOWED_CURRENT_STATUSES = Map.of(
            "SHIPPING", Set.of("PACKED", "SHIPPING", "FAILED"),
            "DELIVERED", Set.of("PACKED", "SHIPPING", "FAILED", "DELIVERED"),
            "FAILED", Set.of("PACKED", "SHIPPING", "FAILED"),
            "CANCELLED", Set.of("PACKED", "SHIPPING", "FAILED", "CANCELLED"));

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final OrderPaymentService orderPaymentService;
    private final OrderCancellationService orderCancellationService;
    private final RefundService refundService;
    private final UserNotificationService userNotificationService;
    private final AdminNotificationService adminNotificationService;

    @Transactional
    public SyncResult applyStatus(String trackingCode, String ghnStatus, String source) {
        String normalizedGhnStatus = ghnStatus == null ? null : ghnStatus.trim().toLowerCase();
        String newStatus = GHN_STATUS_MAP.get(normalizedGhnStatus);
        if (newStatus == null) {
            return SyncResult.UNSUPPORTED_STATUS;
        }

        Order order = orderRepository.findByTrackingCodeForUpdate(trackingCode).orElse(null);
        if (order == null) {
            return SyncResult.ORDER_NOT_FOUND;
        }

        String currentStatus = normalize(order.getOrderStatus());
        if (newStatus.equals(currentStatus)) {
            repairFinancialStateIfNeeded(order, newStatus, trackingCode, source);
            requestExceptionalRefundIfNeeded(order, normalizedGhnStatus, trackingCode, source);
            return SyncResult.NO_CHANGE;
        }

        if (!isAllowedTransition(currentStatus, newStatus)) {
            log.warn("[GHN {}] Bỏ qua chuyển trạng thái không hợp lệ cho đơn {}: {} -> {} (GHN={})",
                    source, order.getOrderCode(), currentStatus, newStatus, normalizedGhnStatus);
            return SyncResult.INVALID_TRANSITION;
        }

        if ("CANCELLED".equals(newStatus)) {
            orderCancellationService.cancelAndRestore(
                    order, "GHN " + source + " báo hủy vận đơn. Tracking: " + trackingCode);
        } else {
            order.setOrderStatus(newStatus);
        }
        repairFinancialStateIfNeeded(order, newStatus, trackingCode, source);
        requestExceptionalRefundIfNeeded(order, normalizedGhnStatus, trackingCode, source);
        createHistory(order, newStatus, normalizedGhnStatus, source);
        orderRepository.save(order);
        // Marcus thêm: trạng thái do GHN cập nhật cũng đi qua cùng chuông khách,
        // không phụ thuộc khách đang mở trang chi tiết đơn.
        userNotificationService.createOrderStatusNotification(order, newStatus, null);
        if ("FAILED".equals(newStatus) || "CANCELLED".equals(newStatus)) {
            adminNotificationService.createAndSendNotification(
                    "ORDER_" + newStatus,
                    "GHN cập nhật đơn " + order.getOrderCode(),
                    "Đơn cần kiểm tra. Trạng thái GHN: " + normalizedGhnStatus + ".",
                    order.getOrderCode());
        }
        return SyncResult.UPDATED;
    }

    private boolean isAllowedTransition(String currentStatus, String newStatus) {
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)
                || "DELIVERED".equals(currentStatus)) {
            return false;
        }
        return ALLOWED_CURRENT_STATUSES.getOrDefault(newStatus, Set.of()).contains(currentStatus);
    }

    private void repairFinancialStateIfNeeded(
            Order order, String newStatus, String trackingCode, String source) {
        if ("DELIVERED".equals(newStatus)) {
            orderPaymentService.handleCodDelivered(
                    order, "GHN_" + source + "_DELIVERED:" + trackingCode);
            orderRepository.save(order);
            return;
        }

    }

    private void requestExceptionalRefundIfNeeded(
            Order order, String ghnStatus, String trackingCode, String source) {
        // Marcus thêm: lost/damage chỉ mở yêu cầu refund, không hoàn kho như luồng
        // returned.
        if (!Set.of("lost", "damage").contains(ghnStatus)) {
            return;
        }
        refundService.requestSystemRefundIfEligible(
                order,
                "GHN " + source + " báo " + ghnStatus + ". Tracking: " + trackingCode);
    }

    private void createHistory(Order order, String status, String ghnStatus, String source) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setTitle(getTitle(status));
        history.setNote("Cập nhật từ GHN " + source + ". Status GHN: " + ghnStatus);
        historyRepository.save(history);
    }

    private String getTitle(String status) {
        return switch (status) {
            case "SHIPPING" -> "Đơn hàng đang được giao";
            case "DELIVERED" -> "Giao hàng thành công";
            case "FAILED" -> "Giao hàng không thành công";
            case "CANCELLED" -> "Đơn hàng đã hủy";
            default -> "Cập nhật trạng thái";
        };
    }

    private String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }
}
