package com.fpoly.marcusstore.repository.auth;

import com.fpoly.marcusstore.entity.auth.Role;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findById(Integer id);

     @Query("""
        SELECT DISTINCT r
        FROM Role r
        LEFT JOIN FETCH r.permissions
        WHERE r.roleName = :roleName
        """)
    Optional<Role> findByRoleNameWithPermissions(@Param("roleName") String roleName);
}