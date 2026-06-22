package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.entity.contact.AdminNotification;
import com.fpoly.marcusstore.repository.contact.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NotificationController {

    private final AdminNotificationRepository notifRepo;

    // danh sách 20 thông báo chưa đọc mới nhất để hiển thị lên Dropdown chuông
    @GetMapping("/unread")
    public ApiResponse<List<AdminNotification>> getUnreadNotifications() {
        List<AdminNotification> unreadList = notifRepo.findTop20ByIsReadFalseOrderByCreatedAtDesc();
        return ApiResponse.success(unreadList);
    }

    // đánh dấu tất cả thông báo là đã đọc
    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead() {
        notifRepo.markAllAsRead();
        return ApiResponse.success(null);
    }
}