package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.AdminNotificationResponse;
import com.fpoly.marcusstore.entity.contact.AdminNotification;
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

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private static final String ADMIN_NOTIFICATION_TOPIC = "/topic/admin/notifications";
    private static final int MAX_PAGE_SIZE = 30;

    private final AdminNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationsForAdmin(int page, int size, boolean unreadOnly) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // Marcus sửa: repository đã khai báo OrderByCreatedAtDesc, không chồng thêm
        // ORDER BY từ Pageable.
        PageRequest pageable = PageRequest.of(safePage, safeSize);

        Page<AdminNotification> result = unreadOnly
                ? notificationRepository.findByIsReadFalseOrderByCreatedAtDesc(pageable)
                : notificationRepository.findAllByOrderByCreatedAtDesc(pageable);

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
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        AdminNotification saved = notificationRepository.saveAndFlush(notification);
        AdminNotificationResponse data = toResponse(saved);

        // Marcus sửa: chỉ phát realtime sau khi transaction nghiệp vụ đã commit thành
        // công.
        sendAfterCommit(Map.of("event", "NEW", "data", data));
    }

    private AdminNotificationResponse toResponse(AdminNotification notification) {
        return AdminNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .isRead(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
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
