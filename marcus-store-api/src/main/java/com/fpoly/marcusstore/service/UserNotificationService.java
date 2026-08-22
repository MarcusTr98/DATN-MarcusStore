package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.UserNotificationResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.contact.UserNotification;
import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.entity.shopping.OrderAssignment;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import com.fpoly.marcusstore.repository.contact.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import com.fpoly.marcusstore.utils.NotificationRegistry;

@Service
@RequiredArgsConstructor
// Marcus thêm luồng lưu + phát realtime notification cho đúng tài khoản khách.
public class UserNotificationService {
    private static final Map<String, String> ORDER_STATUS_TITLES = Map.ofEntries(
            Map.entry("PENDING", "Đặt hàng thành công"),
            Map.entry("CONFIRMED", "Admin đã xác nhận đơn"),
            Map.entry("PROCESSING", "Đang chuẩn bị hàng"),
            Map.entry("READY_FOR_PICKUP", "Sẵn sàng nhận tại cửa hàng"),
            Map.entry("PACKED", "Đơn hàng đã đóng gói"),
            Map.entry("SHIPPING", "Đơn hàng đang được giao"),
            Map.entry("DELIVERED", "Giao hàng thành công"),
            Map.entry("COMPLETED", "Đơn hàng hoàn thành"),
            Map.entry("CANCELLED", "Đơn hàng đã hủy"),
            Map.entry("FAILED", "Giao hàng chưa thành công"));
    private static final Map<String, String> WARRANTY_STATUS_TITLES = Map.ofEntries(
            Map.entry("PENDING", "Yêu cầu bảo hành đã được gửi"),
            Map.entry("CONFIRMED", "Admin đã xác nhận yêu cầu bảo hành"),
            Map.entry("APPROVED", "Yêu cầu bảo hành được duyệt"),
            Map.entry("REJECTED", "Yêu cầu bảo hành bị từ chối"));
    private static final int MAX_PAGE_SIZE = 30;
    private final UserNotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(Integer userId, int page, int size) {
        Page<UserNotification> result = repository.findByUserUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE)));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("list", result.getContent().stream().map(this::toResponse).toList());
        response.put("unreadCount", repository.countByUserUserIdAndIsReadFalse(userId));
        response.put("hasMore", result.hasNext());
        return response;
    }

    @Transactional
    public void create(User user, String type, String title, String message, String referenceId) {
        create(user, type, title, message, referenceId, title);
    }

    @Transactional
    public void notifyOrderAssigned(OrderAssignment assignment) {
        if (assignment == null || assignment.getOrder() == null || assignment.getStaff() == null)
            return;
        Order order = assignment.getOrder();
        String mode = switch (String.valueOf(assignment.getAssignmentType()).toUpperCase(Locale.ROOT)) {
            case "AUTO" -> "tự động";
            case "SELF" -> "do bạn chủ động nhận";
            default -> "thủ công";
        };
        create(
                assignment.getStaff(),
                "STAFF_ORDER_ASSIGNED",
                "Bạn được giao đơn mới",
                "Đơn " + order.getOrderCode() + " vừa được phân công " + mode + " cho bạn.",
                order.getOrderCode(),
                String.valueOf(assignment.getAssignmentId()));
    }

    private void create(User user, String type, String title, String message, String referenceId,
            String discriminator) {
        if (user == null || user.getUserId() == null)
            return;
        String eventKey = NotificationRegistry.eventKey(
                "USER_" + user.getUserId(), type, referenceId, discriminator);
        java.util.Optional<UserNotification> existing = repository.findByEventKey(eventKey);
        if (existing != null && existing.isPresent())
            return;
        NotificationRegistry.Metadata metadata = NotificationRegistry.forUser(type, referenceId);
        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setEventKey(eventKey);
        notification.setCategory(metadata.category());
        notification.setIcon(metadata.icon());
        notification.setDeepLink(metadata.deepLink());
        notification.setExpiresAt(LocalDateTime.now().plusDays(120));
        UserNotificationResponse data = toResponse(repository.saveAndFlush(notification));
        sendAfterCommit(user.getUsername(), Map.of("event", "NEW", "data", data));
    }

    // Marcus thêm một cổng phát chuông thống nhất cho toàn bộ vòng đời đơn hàng,
    // tránh mỗi service tự viết title/type khác nhau.
    @Transactional
    public void createOrderStatusNotification(Order order, String status, String detail) {
        if (order == null || status == null)
            return;
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        String title = ORDER_STATUS_TITLES.get(normalized);
        if (title == null)
            return;
        String message = detail == null || detail.isBlank()
                ? "Đơn " + order.getOrderCode() + " vừa được cập nhật: " + title + "."
                : detail;
        create(order.getUser(), "ORDER_" + normalized, title, message, order.getOrderCode());
    }

    // Marcus thêm: phát chuông cho khách khi admin cập nhật trạng thái bảo hành.
    @Transactional
    public void notifyWarrantyStatusChanged(WarrantyReturn warranty, WarrantyStatus newStatus, String adminNote) {
        if (warranty == null || warranty.getUser() == null || newStatus == null)
            return;
        String type = "WARRANTY_" + newStatus.name();
        String title = WARRANTY_STATUS_TITLES.getOrDefault(newStatus.name(), "Cập nhật bảo hành");
        String message = adminNote != null && !adminNote.isBlank()
                ? adminNote
                : "Yêu cầu bảo hành #" + warranty.getWarrantyId() + " vừa được cập nhật: " + title + ".";
        create(warranty.getUser(), type, title, message, warranty.getOrderItem().getOrder().getOrderCode());
    }

    @Transactional
    public void markRead(Integer userId, Integer id) {
        UserNotification notification = repository.findById(id)
                .filter(item -> item.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy thông báo."));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            repository.save(notification);
        }
    }

    @Transactional
    public void markAllRead(Integer userId) {
        repository.markAllAsRead(userId);
    }

    private UserNotificationResponse toResponse(UserNotification item) {
        return UserNotificationResponse.builder()
                .id(item.getId()).type(item.getType()).title(item.getTitle())
                .message(item.getMessage()).referenceId(item.getReferenceId())
                .category(item.getCategory()).icon(item.getIcon()).deepLink(item.getDeepLink())
                .isRead(Boolean.TRUE.equals(item.getIsRead())).createdAt(item.getCreatedAt())
                .expiresAt(item.getExpiresAt()).build();
    }

    // Marcus thêm: chuông khách quá hạn được dọn định kỳ, không để bảng tăng vô
    // hạn.
    @Scheduled(cron = "0 35 3 * * *")
    @Transactional
    public void cleanupExpiredNotifications() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private void sendAfterCommit(String username, Object payload) {
        // Marcus sửa bảo mật: dùng user destination của STOMP, khách không thể đổi
        // userId trên topic để nghe chuông của tài khoản khác.
        Runnable send = () -> messagingTemplate.convertAndSendToUser(username, "/queue/notifications", payload);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }
}
