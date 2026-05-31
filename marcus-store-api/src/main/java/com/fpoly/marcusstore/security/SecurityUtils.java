package com.fpoly.marcusstore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    // Lấy toàn bộ thông tin User đang đăng nhập
    public static CustomUserDetails getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    // Tiện ích lấy nhanh User ID cho thành viên
    public static Integer getCurrentUserId() {
        CustomUserDetails userDetails = getCurrentUserPrincipal();
        if (userDetails != null) {
            return userDetails.getUserId();
        }
        throw new RuntimeException("Lỗi bảo mật: Người dùng chưa đăng nhập hoặc Token không hợp lệ!");
    }
}