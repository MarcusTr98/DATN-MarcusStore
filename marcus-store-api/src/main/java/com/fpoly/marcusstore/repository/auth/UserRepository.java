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

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN FETCH u.role r
            LEFT JOIN FETCH r.permissions
            WHERE u.username = :username
            """)
    Optional<User> findByUsername(@Param("username") String username);

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

    // Lấy tất cả users có role cụ thể (roleId = 3 cho CUSTOMER)
    List<User> findByRoleRoleId(Integer roleId);

    @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId AND " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> findByRoleRoleIdAndKeyword(
            @Param("roleId") Integer roleId,
            @Param("keyword") String keyword);
}