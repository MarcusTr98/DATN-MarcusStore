package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminNotificationService {

    @Autowired
    private AdminNotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationsForAdmin() {
        List<AdminNotification> list = notificationRepository.findTop20ByIsReadFalseOrderByCreatedAtDesc();
        long unreadCount = notificationRepository.countByIsReadFalse();

        return Map.of(
                "list", list,
                "unreadCount", unreadCount);
    }

    // 2. Logic đọc thông báo đơn lẻ
    @Transactional
    public void markAsRead(Integer id) {
        notificationRepository.findById(id).ifPresent(notif -> {
            notif.setIsRead(true);
            notificationRepository.save(notif);
        });
    }

    // 3. Logic đọc toàn bộ thông báo
    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead();
    }

    // 4. Logic bắn Real-time cũ
    @Transactional
    public void createAndSendNotification(String type, String title, String message, String referenceId) {
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        AdminNotification savedNotification = notificationRepository.saveAndFlush(notification);

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", savedNotification.getId());
        payload.put("type", savedNotification.getType());
        payload.put("title", savedNotification.getTitle());
        payload.put("message", savedNotification.getMessage());
        payload.put("referenceId", savedNotification.getReferenceId());
        payload.put("isRead", savedNotification.getIsRead());

        if (savedNotification.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            payload.put("time", savedNotification.getCreatedAt().format(formatter));
        } else {
            payload.put("time", "Vừa xong");
        }

        messagingTemplate.convertAndSend("/topic/admin/notifications", payload);
    }
}