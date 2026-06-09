package com.fpoly.marcusstore.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpoly.marcusstore.entity.auth.EmailOTP;

public interface EmailOTPRepository
        extends JpaRepository<EmailOTP, Integer> {

    Optional<EmailOTP> findTopByEmailOrderByOtpIdDesc(String email);

    void deleteByEmail(String email);
}