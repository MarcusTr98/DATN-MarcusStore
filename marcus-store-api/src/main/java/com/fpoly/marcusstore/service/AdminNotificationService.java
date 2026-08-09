package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.AdminNotificationResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import com.fpoly.marcusstore.utils.NotificationRegistry;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private static final String ADMIN_NOTIFICATION_TOPIC = "/topic/admin/notifications";
    private static final int MAX_PAGE_SIZE = 30;

    private final AdminNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Map<String, Object> getNotificationsForAdmin(int page, int size, boolean unreadOnly) {
        return getNotificationsForAdmin(page, size, unreadOnly, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationsForAdmin(int page, int size, boolean unreadOnly, String category) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // Marcus sửa: repository đã khai báo OrderByCreatedAtDesc, không chồng thêm
        // ORDER BY từ Pageable.
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        String normalizedCategory = category == null ? "" : category.trim().toUpperCase();
        Page<AdminNotification> result;
        if (unreadOnly) {
            result = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc(pageable);
        } else if (java.util.Set.of("INFO", "WARNING", "ACTION_REQUIRED").contains(normalizedCategory)) {
            result = notificationRepository.findByCategoryOrderByCreatedAtDesc(normalizedCategory, pageable);
        } else {
            result = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("list", result.getContent().stream().map(this::toResponse).toList());
        response.put("unreadCount", notificationRepository.countByIsReadFalse());
        response.put("page", result.getNumber());
        response.put("totalPages", result.getTotalPages());
        response.put("totalElements", result.getTotalElements());
        response.put("hasMore", result.hasNext());
        return response;
    }

    @Transactional
    public void markAsRead(Integer id) {
        AdminNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Không tìm thấy thông báo."));

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        sendAfterCommit(Map.of("event", "READ", "id", id));
    }

    @Transactional
    public void markAllAsRead() {
        int updated = notificationRepository.markAllAsRead();
        if (updated > 0) {
            sendAfterCommit(Map.of("event", "READ_ALL"));
        }
    }

    @Transactional
    public void createAndSendNotification(String type, String title, String message, String referenceId) {
        String eventKey = NotificationRegistry.eventKey("ADMIN", type, referenceId, title);
        java.util.Optional<AdminNotification> existing = notificationRepository.findByEventKey(eventKey);
        if (existing != null && existing.isPresent()) {
            return;
        }
        NotificationRegistry.Metadata metadata = NotificationRegistry.forAdmin(type, referenceId);
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setEventKey(eventKey);
        notification.setCategory(metadata.category());
        notification.setIcon(metadata.icon());
        notification.setDeepLink(metadata.deepLink());
        notification.setExpiresAt(LocalDateTime.now().plusDays(
                NotificationRegistry.ACTION_REQUIRED.equals(metadata.category()) ? 180 : 90));
        notification.setIsRead(false);

        AdminNotification saved = notificationRepository.saveAndFlush(notification);
        AdminNotificationResponse data = toResponse(saved);

        // Marcus sửa: chỉ phát realtime sau khi transaction nghiệp vụ đã commit thành
        // công.
        sendAfterCommit(Map.of("event", "NEW", "data", data));
    }

    // cổng phát chuông khi khách gửi yêu cầu bảo hành mới,
    // dùng lại createAndSendNotification nên đảm bảo đúng pattern sau-commit.
    public void notifyWarrantyCreated(WarrantyReturn warranty, User user) {
        String title = "Yêu cầu bảo hành mới";
        String message = String.format(
                "Khách hàng %s vừa gửi yêu cầu bảo hành cho sản phẩm.",
                warranty.getUser().getFullName());
        createAndSendNotification("WARRANTY_REQUEST", title, message,
                String.valueOf(warranty.getWarrantyId()));
    }

    private AdminNotificationResponse toResponse(AdminNotification notification) {
        return AdminNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .category(notification.getCategory())
                .icon(notification.getIcon())
                .deepLink(notification.getDeepLink())
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }

    // Marcus thêm: dọn chuông đã quá hạn mỗi đêm; sự kiện cần xử lý được giữ lâu
    // hơn nhờ expiresAt đã tính lúc tạo.
    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void cleanupExpiredNotifications() {
        notificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private void sendAfterCommit(Object payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_TOPIC, payload);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend(ADMIN_NOTIFICATION_TOPIC, payload);
            }
        });
    }
}
