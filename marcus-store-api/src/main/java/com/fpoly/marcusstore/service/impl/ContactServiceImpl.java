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
import java.time.LocalDateTime;
import java.util.Set;

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
        contact.setCustomerName(cleanText(request.getName()));
        contact.setPhoneNumber(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setMessage(cleanText(request.getMessage()));
        contact.setStatus("NEW");
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
        updateStatus(contactId, "RESOLVED");
    }

    @Transactional
    public void updateStatus(Integer contactId, String rawStatus) {
        ContactRequest contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu liên hệ."));
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        if (!Set.of("NEW", "IN_PROGRESS", "RESOLVED", "SPAM").contains(status)) {
            throw new IllegalArgumentException("Trạng thái liên hệ không hợp lệ.");
        }
        contact.setStatus(status);
        contact.setHandledBy(SecurityUtils.getCurrentUsername());
        if ("IN_PROGRESS".equals(status) && contact.getProcessingStartedAt() == null) {
            contact.setProcessingStartedAt(LocalDateTime.now());
        }
        contact.setResolvedAt(Set.of("RESOLVED", "SPAM").contains(status) ? LocalDateTime.now() : null);
        contactRepo.save(contact);
    }

    private static String cleanText(String raw) {
        if (raw == null)
            return null;
        // Marcus thêm: Contact chỉ nhận văn bản thuần; Vue cũng escape khi render.
        return raw.replaceAll("<[^>]*>", " ").replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
    }
}
