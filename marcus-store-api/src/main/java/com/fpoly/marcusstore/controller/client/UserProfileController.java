package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ProfileRequestDTO;
import com.fpoly.marcusstore.dto.response.ProfileResponseDTO;
import com.fpoly.marcusstore.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/client/profile")
@RequiredArgsConstructor
public class UserProfileController {

        private final UserProfileService userProfileService;

        @GetMapping
        public ResponseEntity<?> getMyProfile() {
                try {
                        ProfileResponseDTO res = userProfileService.getMyProfile();
                        // Tự đóng gói JSON bằng Map để né mọi rủi ro từ class ApiResponse
                        Map<String, Object> response = new HashMap<>();
                        response.put("code", 200);
                        response.put("message", "Thành công");
                        response.put("data", res);
                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        e.printStackTrace(); // Bắt buộc in lỗi đỏ lòm ra màn hình Console Java

                        Map<String, Object> errorDetail = new HashMap<>();
                        errorDetail.put("code", 500);
                        errorDetail.put("message",
                                        "Lỗi nội bộ: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        return ResponseEntity.status(500).body(errorDetail); // Ép trả về JSON lỗi
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
                        Map<String, Object> errorDetail = new HashMap<>();
                        errorDetail.put("code", 500);
                        errorDetail.put("message",
                                        "Lỗi nội bộ: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                        return ResponseEntity.status(500).body(errorDetail);
                }
        }
}