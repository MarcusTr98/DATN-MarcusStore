package com.fpoly.marcusstore.service;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.dto.request.RegisterRequest;
import com.fpoly.marcusstore.dto.request.VerifyOtpRequest;
import com.fpoly.marcusstore.entity.auth.EmailOTP;
import com.fpoly.marcusstore.entity.auth.PendingRegistration;
import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.repository.auth.EmailOTPRepository;
import com.fpoly.marcusstore.repository.auth.PendingRegistrationRepository;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class AuthRegisterService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailOTPRepository otpRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EntityManager entityManager; 
    private final CartRepository cartRepository; 
       @Transactional
    public void requestRegister(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

   
        entityManager.createNativeQuery(
            "DELETE FROM PendingRegistrations WHERE email = :email"
        ).setParameter("email", request.getEmail()).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM PendingRegistrations WHERE username = :username"
        ).setParameter("username", request.getUsername()).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM EmailOtps WHERE email = :email"
        ).setParameter("email", request.getEmail()).executeUpdate();

        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(request.getUsername());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setEmail(request.getEmail());
        pending.setPhoneNumber(request.getPhoneNumber());
        pending.setFullName(request.getFullName());
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        pendingRepository.save(pending);

        String otp = String.format("%06d",
                ThreadLocalRandom.current().nextInt(100000, 1000000));

        EmailOTP emailOtp = new EmailOTP();
        emailOtp.setEmail(request.getEmail());
        emailOtp.setOtpCode(otp);
        emailOtp.setCreatedAt(LocalDateTime.now());
        emailOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(emailOtp);

        emailService.sendOtp(request.getEmail(), otp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public String verifyRegister(VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        PendingRegistration pending = pendingRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đăng ký"));

        Role role = roleRepository
                .findByRoleName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        User user = new User();
        user.setRole(role);
        user.setUsername(pending.getUsername());
        user.setPasswordHash(pending.getPasswordHash());
        user.setEmail(pending.getEmail());
        user.setPhoneNumber(pending.getPhoneNumber());
        user.setFullName(pending.getFullName());
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        Cart cart = new Cart();
cart.setUser(user);
cart.setCreatedAt(LocalDateTime.now());
cartRepository.save(cart);
        pendingRepository.deleteByEmail(request.getEmail());

        return "Đăng ký thành công";
    }
}