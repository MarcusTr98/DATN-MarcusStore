package com.fpoly.marcusstore.repository.auth;

import com.fpoly.marcusstore.entity.auth.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
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

    @Query("""
            SELECT u FROM User u
            JOIN u.role r
            WHERE (
                :keyword IS NULL
                OR LOWER(u.fullName) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(u.email) LIKE CONCAT('%', :keyword, '%')
                OR LOWER(u.username) LIKE CONCAT('%', :keyword, '%')
                OR u.phoneNumber LIKE CONCAT('%', :keyword, '%')
            )
            AND (:rolesEmpty = true OR r.roleName IN :roles)
            """)
    Page<User> findAllByKeywordAndRoles(
            @Param("keyword") String keyword,
            @Param("roles") List<String> roles,
            @Param("rolesEmpty") boolean rolesEmpty,
            Pageable pageable);
}
