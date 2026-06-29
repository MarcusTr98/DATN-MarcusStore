package com.fpoly.marcusstore.service.impl;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Service;

import com.fpoly.marcusstore.dto.request.UpdateRolePermissionRequest;
import com.fpoly.marcusstore.dto.response.PermissionResponse;
import com.fpoly.marcusstore.entity.auth.Permission;
import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.repository.auth.PermissionRepository;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.service.PermissionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl
        implements PermissionService {

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    @Override
    public List<PermissionResponse> getAll() {

        return permissionRepository.findAll()

                .stream()

                .map(p -> new PermissionResponse(

                        p.getPermissionId(),

                        p.getPermissionName(),

                        p.getDescription(),

                        p.getModuleName()

                ))

                .toList();

    }

    @Override
    public List<Integer> getPermissionOfRole(Integer roleId) {

        Role role = roleRepository.findById(roleId)

                .orElseThrow();

        return role.getPermissions()

                .stream()

                .map(Permission::getPermissionId)

                .toList();

    }

    @Override
    public void updateRolePermission(Integer roleId, UpdateRolePermissionRequest request) {

        Role role = roleRepository.findById(roleId).orElseThrow();

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissionIds()));
        if (role.getRoleName().equals("ADMIN")) {
            throw new RuntimeException("Không được sửa quyền ADMIN");
        }
        role.setPermissions(permissions);

        roleRepository.save(role);

    }

}