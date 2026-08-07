package com.fpoly.marcusstore.repository.auth;

import com.fpoly.marcusstore.entity.auth.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    List<Permission> findByModuleName(String moduleName);

    // Lấy toàn bộ quyền thuộc nhiều module
    List<Permission> findByModuleNameIn(Set<String> moduleNames);
}