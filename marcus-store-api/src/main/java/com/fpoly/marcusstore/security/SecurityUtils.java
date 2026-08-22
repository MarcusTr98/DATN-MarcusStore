package com.fpoly.marcusstore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Arrays;

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

    // Marcus thêm: lưu snapshot người thực hiện cho audit các module vận hành.
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority -> Arrays.stream(roles)
                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                        .anyMatch(authority::equals));
    }
}
