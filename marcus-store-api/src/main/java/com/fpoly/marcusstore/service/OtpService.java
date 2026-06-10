package com.fpoly.marcusstore.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;


import com.fpoly.marcusstore.entity.auth.EmailOTP;
import com.fpoly.marcusstore.repository.auth.EmailOTPRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {
 private final EmailOTPRepository emailOtpRepository;

    private final OtpAttemptService otpAttemptService;

    public void verifyOtp(String email, String otpInput) {

        EmailOTP otp = emailOtpRepository
                .findByEmailAndExpiredAtAfter(email, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("OTP không tồn tại hoặc đã hết hạn"));

        if (otp.getAttemptCount() >= 3) {
            otpAttemptService.deleteOtp(otp.getOtpId());
            throw new RuntimeException("OTP đã bị khóa do nhập sai quá 3 lần");
        }

        if (otp.getOtpCode() == null ||
            !otp.getOtpCode().trim().equals(otpInput.trim())) {

            int currentAttempt = otpAttemptService.incrementAttempt(otp.getOtpId());
            int remain = 3 - currentAttempt;

            if (remain <= 0) {
                otpAttemptService.deleteOtp(otp.getOtpId());
                throw new RuntimeException("OTP đã bị khóa do nhập sai quá 3 lần");
            }

            throw new RuntimeException("OTP sai. Còn " + remain + " lần thử");
        }

        otpAttemptService.deleteOtp(otp.getOtpId());
    }
}