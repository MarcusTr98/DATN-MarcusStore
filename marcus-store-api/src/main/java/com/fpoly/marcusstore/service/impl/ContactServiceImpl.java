package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateContactRequest;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl {

    private final ContactRequestRepository contactRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void submitContact(CreateContactRequest request) {
        // 1. Lưu thông tin vào Database
        ContactRequest contact = new ContactRequest();
        contact.setCustomerName(request.getName());
        contact.setPhoneNumber(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setMessage(request.getMessage());
        contact.setStatus("PENDING"); // chờ xử lý
        contactRepository.save(contact);

        // 2. Tạo nội dung thông báo khớp với cấu trúc Frontend Admin
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", contact.getContactId());
        notifData.put("type", "CONTACT");
        notifData.put("title", "Yêu cầu hỗ trợ mới");
        notifData.put("message", "Khách hàng " + contact.getCustomerName() + " vừa gửi form liên hệ.");
        notifData.put("time", "Vừa xong");

        // 3. Đẩy vào kênh WebSocket
        messagingTemplate.convertAndSend("/topic/admin/notifications", notifData);
    }
}