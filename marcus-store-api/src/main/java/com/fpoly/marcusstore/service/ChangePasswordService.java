package com.fpoly.marcusstore.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fpoly.marcusstore.dto.request.ChangePasswordRequest;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePasswordService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
 public void changePassword(
            ChangePasswordRequest request) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String username =
                authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash())) {

            throw new RuntimeException(
                    "Mật khẩu hiện tại không chính xác");
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new RuntimeException(
                    "Mật khẩu xác nhận không khớp");
        }

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepository.save(user);
    }
}
