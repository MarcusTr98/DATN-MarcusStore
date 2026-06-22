package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminNotificationService {

    @Autowired
    private AdminNotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void createAndSendNotification(String type, String title, String message, String referenceId) {
        // 1. Lưu thông báo vào Database
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);

        AdminNotification savedNotification = notificationRepository.saveAndFlush(notification); // Dùng saveAndFlush để
                                                                                                 // ép ID sinh ra ngay
                                                                                                 // lập tức

        // 2. TẠO DTO (MAP) ĐỂ GỬI QUA WEBSOCKET (Đóng gói sạch sẽ, an toàn)
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", savedNotification.getId());
        payload.put("type", savedNotification.getType());
        payload.put("title", savedNotification.getTitle());
        payload.put("message", savedNotification.getMessage());
        payload.put("referenceId", savedNotification.getReferenceId());
        payload.put("isRead", savedNotification.getIsRead());

        // Format thời gian thành chuỗi thân thiện cho Frontend
        if (savedNotification.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            payload.put("time", savedNotification.getCreatedAt().format(formatter));
        } else {
            payload.put("time", "Vừa xong");
        }

        // 3. Bắn thông báo an toàn qua WebSocket
        messagingTemplate.convertAndSend("/topic/admin/notifications", payload);
    }
}