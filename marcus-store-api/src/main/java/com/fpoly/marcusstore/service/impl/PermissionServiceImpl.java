package com.fpoly.marcusstore.service.impl;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.stereotype.Service;

import com.fpoly.marcusstore.dto.request.UpdateUserPermissionRequest;
import com.fpoly.marcusstore.dto.response.PermissionResponse;
import com.fpoly.marcusstore.entity.auth.Permission;
import com.fpoly.marcusstore.repository.auth.PermissionRepository;
import com.fpoly.marcusstore.service.PermissionService;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl
        implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
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
public List<Integer> getPermissionOfUser(Integer userId) {

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy nhân viên"));

    return user.getPermissions()
            .stream()
            .map(Permission::getPermissionId)
            .toList();
}

@Override
public void updateUserPermission(
        Integer userId,
        UpdateUserPermissionRequest request) {

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy nhân viên"));

    // Chỉ cho STAFF phân quyền riêng
    if (!"STAFF".equalsIgnoreCase(user.getRole().getRoleName())) {
        throw new RuntimeException("Chỉ được phân quyền cho STAFF");
    }

    Set<Permission> permissions =
            new HashSet<>(
                    permissionRepository.findAllById(request.getPermissionIds())
            );

    user.setPermissions(permissions);

    userRepository.save(user);
}

}