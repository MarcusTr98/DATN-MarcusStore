package com.fpoly.marcusstore.repository.auth;

import com.fpoly.marcusstore.entity.auth.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

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
SELECT u
FROM User u
JOIN u.role r
WHERE
(
    :keyword IS NULL
    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
    OR u.phoneNumber LIKE CONCAT('%', :keyword, '%')
)
AND
(
    :rolesEmpty = true
    OR r.roleName IN :roles
)
AND
(
    :status IS NULL
    OR u.isActive = :status
)
AND
(
    :emailVerified IS NULL
    OR u.emailVerified = :emailVerified
)
""")
Page<User> findAllByKeywordAndRoles(
        @Param("keyword") String keyword,
        @Param("roles") List<String> roles,
        @Param("rolesEmpty") boolean rolesEmpty,
        @Param("status") Boolean status,
        @Param("emailVerified") Boolean emailVerified,
        Pageable pageable
);
        // Lấy tất cả users có role cụ thể (roleId = 3 cho CUSTOMER)
        List<User> findByRoleRoleId(Integer roleId);

        @Query("SELECT u FROM User u WHERE u.role.roleId = :roleId AND " +
                        "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<User> findByRoleRoleIdAndKeyword(
                        @Param("roleId") Integer roleId,
                        @Param("keyword") String keyword);

        @Query("""
SELECT DISTINCT u
FROM User u
JOIN FETCH u.role r
LEFT JOIN FETCH r.permissions
LEFT JOIN FETCH u.permissions
WHERE u.email = :email
""")
Optional<User> findByEmailWithRole(@Param("email") String email);
@Query("""
SELECT u
FROM User u
JOIN FETCH u.role r
LEFT JOIN FETCH r.permissions
LEFT JOIN FETCH u.permissions
WHERE u.googleAccountId = :googleAccountId
""")
Optional<User> findByGoogleAccountId(@Param("googleAccountId") String googleAccountId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
SELECT DISTINCT u
FROM User u
JOIN u.role r
LEFT JOIN r.permissions rolePermission
LEFT JOIN u.permissions userPermission
WHERE r.roleName = 'STAFF'
  AND u.isActive = true
  AND (rolePermission.permissionName = 'ORDER_UPDATE' OR userPermission.permissionName = 'ORDER_UPDATE')
ORDER BY u.userId
""")
        List<User> findActiveStaffWithOrderUpdatePermissionForAssignment();

        @Query("""
SELECT DISTINCT u
FROM User u
JOIN u.role r
LEFT JOIN r.permissions rolePermission
LEFT JOIN u.permissions userPermission
WHERE r.roleName = 'STAFF'
  AND u.isActive = true
  AND (rolePermission.permissionName = 'ORDER_UPDATE' OR userPermission.permissionName = 'ORDER_UPDATE')
ORDER BY u.userId
""")
        List<User> findActiveStaffWithOrderUpdatePermission();

        @Query("""
SELECT DISTINCT u
FROM User u
JOIN u.role r
LEFT JOIN r.permissions rolePermission
LEFT JOIN u.permissions userPermission
WHERE u.userId = :staffId
  AND r.roleName = 'STAFF'
  AND u.isActive = true
  AND (rolePermission.permissionName = 'ORDER_UPDATE' OR userPermission.permissionName = 'ORDER_UPDATE')
""")
        Optional<User> findActiveStaffWithOrderUpdatePermissionById(@Param("staffId") Integer staffId);
// Load user với permissions (dùng cho phân quyền)
@Query("""
SELECT u
FROM User u
LEFT JOIN FETCH u.permissions
WHERE u.userId = :userId
""")
Optional<User> findByIdWithPermissions(@Param("userId") Integer userId);
}
