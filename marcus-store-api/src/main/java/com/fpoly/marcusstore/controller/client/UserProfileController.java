package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ProfileRequestDTO;
import com.fpoly.marcusstore.dto.response.ProfileResponseDTO;
import com.fpoly.marcusstore.service.UserProfileService;
import com.fpoly.marcusstore.service.impl.UserServiceImpl;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/client/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserServiceImpl userServiceImpl;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getMyProfile() {
        try {
            ProfileResponseDTO res = userProfileService.getMyProfile();
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Thành công");
            response.put("data", res);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Lỗi nội bộ: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileRequestDTO request) {
        try {
            ProfileResponseDTO res = userProfileService.updateProfile(request);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Cập nhật thành công");
            response.put("data", res);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Lỗi nội bộ: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Khách tự gửi mail xác thực cho chính mình
    @PostMapping("/send-verify-email")
    public ResponseEntity<?> sendVerifyEmail(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userRepository
                    .findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            userServiceImpl.sendVerifyEmail(user.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "Email xác thực đã được gửi. Vui lòng kiểm tra hộp thư.");
            response.put("data", null);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("code", 500);
            error.put("message", "Lỗi nội bộ: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}