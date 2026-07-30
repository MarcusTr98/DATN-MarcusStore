package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
// Marcus thêm API chuông khách: userId luôn lấy từ token qua SecurityUtils.
public class UserNotificationController {
    private final UserNotificationService service;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.getNotifications(SecurityUtils.getCurrentUserId(), page, size));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable Integer id) {
        service.markRead(SecurityUtils.getCurrentUserId(), id);
        return ApiResponse.success("Đã đọc thông báo.");
    }

    @PutMapping("/mark-all-read")
    public ApiResponse<Void> readAll() {
        service.markAllRead(SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Đã đọc tất cả thông báo.");
    }
}
