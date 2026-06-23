package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateContactRequest;
import com.fpoly.marcusstore.entity.contact.ContactRequest;
import com.fpoly.marcusstore.repository.contact.ContactRequestRepository;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.AdminNotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl {
    private final ContactRequestRepository contactRepo;
    private final AdminNotificationService notificationService;

    @Transactional
    public void submitContact(CreateContactRequest request) {
        // 1. Kiểm tra User ID một cách an toàn (Guest thì để null)
        Integer currentUserId = null;
        CustomUserDetails currentUser = SecurityUtils.getCurrentUserPrincipal();
        if (currentUser != null) {
            currentUserId = currentUser.getUserId();
        }

        // 2. Lưu form liên hệ
        ContactRequest contact = new ContactRequest();
        contact.setUserId(currentUserId); // Có user thì lưu ID, không có thì null
        contact.setCustomerName(request.getName());
        contact.setPhoneNumber(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setMessage(request.getMessage());
        contact.setStatus("PENDING");
        ContactRequest savedContact = contactRepo.save(contact);

        // 3. Gọi Service để tạo, dọn dẹp và bắn thông báo qua WebSocket chuẩn luồng
        String notifTitle = "Yêu cầu hỗ trợ mới";
        String notifMessage = "Khách hàng " + savedContact.getCustomerName() + " vừa gửi form liên hệ.";

        notificationService.createAndSendNotification(
                "CONTACT",
                notifTitle,
                notifMessage,
                String.valueOf(savedContact.getContactId()));
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