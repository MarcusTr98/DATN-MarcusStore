package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateContactRequest;
import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
import com.fpoly.marcusstore.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl {
    private final ContactRequestRepository contactRepo;
    private final AdminNotificationRepository notifRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void submitContact(CreateContactRequest request) {
        // 1. Lưu form liên hệ
        Integer currentUserId = SecurityUtils.getCurrentUserId(); // trả về null nếu là khách vãng lai

        ContactRequest contact = new ContactRequest();
        contact.setUserId(currentUserId);
        contact.setCustomerName(request.getName());
        contact.setPhoneNumber(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setMessage(request.getMessage());
        contact.setStatus("PENDING");
        contactRepo.save(contact);

        // 2. Lưu vào bảng Admin_Notifications để chống mất thông báo khi admin ko onl
        AdminNotification notif = new AdminNotification();
        notif.setType("CONTACT");
        notif.setTitle("Yêu cầu hỗ trợ mới");
        notif.setMessage("Khách hàng " + contact.getCustomerName() + " vừa gửi form liên hệ.");
        notif.setReferenceId(String.valueOf(contact.getContactId()));
        notif.setIsRead(false);
        notifRepo.save(notif);

        // 3. đẩy WebSocket cho admin đang onl (đẩy luôn ID của notification vừa lưu)
        messagingTemplate.convertAndSend("/topic/admin/notifications", notif);
    }

    // Admin đổi trạng thái PENDING => RESOLVED
    @Transactional
    public void resolveContact(Integer contactId) {
        ContactRequest contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
        contact.setStatus("RESOLVED");
        contactRepo.save(contact);
    }
}