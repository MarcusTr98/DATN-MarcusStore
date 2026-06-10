package com.fpoly.marcusstore.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fpoly.marcusstore.entity.auth.PendingRegistration;

public interface PendingRegistrationRepository
        extends JpaRepository<PendingRegistration, Integer> {

    Optional<PendingRegistration> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true) 
    @Query("DELETE FROM PendingRegistration p WHERE p.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true) 
    @Query("DELETE FROM PendingRegistration p WHERE p.username = :username")
    void deleteByUsername(@Param("username") String username);
}