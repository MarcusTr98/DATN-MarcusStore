package com.fpoly.marcusstore.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.dto.request.EmployeePositionRequest;
import com.fpoly.marcusstore.dto.response.EmployeePositionResponse;
import com.fpoly.marcusstore.dto.response.PermissionResponse;
import com.fpoly.marcusstore.entity.auth.EmployeePosition;
import com.fpoly.marcusstore.entity.auth.Permission;
import com.fpoly.marcusstore.repository.auth.EmployeePositionRepository;
import com.fpoly.marcusstore.repository.auth.PermissionRepository;
import com.fpoly.marcusstore.service.EmployeePositionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeePositionServiceImpl implements EmployeePositionService {

    private final EmployeePositionRepository positionRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public List<EmployeePositionResponse> getAll() {

        return positionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public EmployeePositionResponse getById(Integer id) {

        EmployeePosition position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức danh"));

        return mapToResponse(position);
    }

    @Override
    public void create(EmployeePositionRequest request) {

        if (positionRepository.existsByPositionName(request.getPositionName())) {
            throw new RuntimeException("Tên chức danh đã tồn tại");
        }

        Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(request.getPermissions().stream().map(PermissionResponse::getId).toList())
        );

        EmployeePosition position = EmployeePosition.builder()
                .positionName(request.getPositionName())
                .description(request.getDescription())
                .isActive(true)
                .permissions(permissions)
                .build();

        positionRepository.save(position);
    }

    @Override
    public void update(Integer id, EmployeePositionRequest request) {

        EmployeePosition position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức danh"));

        position.setPositionName(request.getPositionName());
        position.setDescription(request.getDescription());

        Set<Permission> permissions = new HashSet<>(
                permissionRepository.findAllById(request.getPermissions().stream().map(PermissionResponse::getId).toList())
        );

        position.setPermissions(permissions);

        positionRepository.save(position);
    }

    @Override
    public void changeStatus(Integer id, Boolean active) {

        EmployeePosition position = positionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chức danh"));

        position.setIsActive(active);

        positionRepository.save(position);
    }

    /**
     * Mapping Entity -> Response
     */
private EmployeePositionResponse mapToResponse(EmployeePosition position) {

    List<PermissionResponse> permissions = position.getPermissions()
            .stream()
            .map(permission -> PermissionResponse.builder()
                    .id(permission.getPermissionId())
                    .permission(permission.getPermissionName())
                    .description(permission.getDescription())
                    .module(permission.getModuleName())
                    .build())
            .toList();

    return EmployeePositionResponse.builder()
            .positionId(position.getPositionId())
            .positionName(position.getPositionName())
            .description(position.getDescription())
            .isActive(position.getIsActive())
            .permissions(permissions)
            .build();
}

}