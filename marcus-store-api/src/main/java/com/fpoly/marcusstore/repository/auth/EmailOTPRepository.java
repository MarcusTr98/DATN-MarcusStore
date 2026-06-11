package com.fpoly.marcusstore.repository.auth;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fpoly.marcusstore.entity.auth.EmailOTP;

public interface EmailOTPRepository
        extends JpaRepository<EmailOTP, Integer> {

    Optional<EmailOTP> findByEmailAndExpiredAtAfter(String email, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true) 
    @Query("DELETE FROM EmailOTP e WHERE e.email = :email")
    void deleteByEmail(@Param("email") String email);
      
}