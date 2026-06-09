package com.fpoly.marcusstore.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpoly.marcusstore.entity.auth.PendingRegistration;

public interface PendingRegistrationRepository
        extends JpaRepository<PendingRegistration, Integer> {

    Optional<PendingRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}