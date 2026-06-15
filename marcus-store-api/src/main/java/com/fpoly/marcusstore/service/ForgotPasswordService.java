package com.fpoly.marcusstore.service;

import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.entity.auth.EmailOTP;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.EmailOTPRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final EmailOTPRepository emailOtpRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void sendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Email không tồn tại"));

        emailOtpRepository.deleteByEmail(email);

        String otp = String.format(
                "%06d",
                new Random().nextInt(1000000)
        );

        EmailOTP emailOTP = new EmailOTP();
        emailOTP.setEmail(user.getEmail());
        emailOTP.setOtpCode(otp);
        emailOTP.setCreatedAt(LocalDateTime.now());
        emailOTP.setExpiredAt(
                LocalDateTime.now().plusMinutes(5)
        );
        emailOTP.setAttemptCount(0);

        emailOtpRepository.save(emailOTP);

        emailService.sendOtp(email, otp);
    }

    public void verifyOtp(String email, String otp) {

        otpService.verifyOtp(email, otp);
    }

    @Transactional
    public void resetPassword(
            String email,
            String newPassword,
            String confirmPassword
    ) {

        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException(
                    "Mật khẩu xác nhận không khớp");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy tài khoản"));

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        emailOtpRepository.deleteByEmail(email);
    }
}