package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
// Marcus sửa: chuông nằm trong layout dùng chung của ADMIN và STAFF.
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@RequiredArgsConstructor
public class NotificationController {

    private final AdminNotificationService notificationService;

    // Lấy danh sách thông báo
    @GetMapping
    public ApiResponse<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(notificationService.getNotificationsForAdmin(page, size, unreadOnly, category));
    }

    // Đánh dấu đọc 1 thông báo
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable("id") Integer id) {
        notificationService.markAsRead(id);
        return ApiResponse.success(null);
    }

    // Đánh dấu đọc tất cả
    @PutMapping("/mark-all-read")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.success(null);
    }
}
