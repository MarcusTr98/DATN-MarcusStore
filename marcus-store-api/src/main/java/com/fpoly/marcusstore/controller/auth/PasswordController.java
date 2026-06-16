package com.fpoly.marcusstore.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fpoly.marcusstore.dto.request.ForgotPasswordRequest;
import com.fpoly.marcusstore.dto.request.ResetPasswordRequest;
import com.fpoly.marcusstore.dto.request.VerifyForgotOtpRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.ForgotPasswordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        forgotPasswordService.sendOtp(
                request.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP đã được gửi tới email"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @Valid @RequestBody VerifyForgotOtpRequest request) {

        forgotPasswordService.verifyOtp(
                request.getEmail(),
                request.getOtp());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP hợp lệ"));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        forgotPasswordService.resetPassword(
                request.getEmail(),
                request.getNewPassword(),
                request.getConfirmPassword());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đổi mật khẩu thành công"));
    }
}