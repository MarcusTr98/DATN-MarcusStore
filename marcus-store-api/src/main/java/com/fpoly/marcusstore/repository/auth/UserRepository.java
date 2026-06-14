package com.fpoly.marcusstore.repository.auth;

import com.fpoly.marcusstore.entity.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Dùng cho Spring Security đăng nhập
    Optional<User> findByUsernameAndIsActiveTrue(String username);

    // Check trùng lặp khi đăng ký
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}