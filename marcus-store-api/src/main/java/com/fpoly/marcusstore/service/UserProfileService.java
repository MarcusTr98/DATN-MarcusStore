package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProfileRequestDTO;
import com.fpoly.marcusstore.dto.response.ProfileResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponseDTO getMyProfile() {
        // 1. Lấy ID (Nếu lỗi ở đây, nó sẽ văng ra Exception báo tên SecurityUtils)
        Integer userId = SecurityUtils.getCurrentUserId();

        // 2. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        // 3. Xử lý Role an toàn (Chống NullPointer)
        String roleName = "CUSTOMER";
        if (user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        // 4. Build DTO
        return ProfileResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roleName(roleName)
                .build();
    }

    @Transactional
    public ProfileResponseDTO updateProfile(ProfileRequestDTO request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        String roleName = "CUSTOMER";
        if (user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        return ProfileResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roleName(roleName)
                .build();
    }
}