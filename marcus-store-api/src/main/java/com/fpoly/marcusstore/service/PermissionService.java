package com.fpoly.marcusstore.service;

import java.util.List;

import com.fpoly.marcusstore.dto.request.UpdateRolePermissionRequest;
import com.fpoly.marcusstore.dto.response.PermissionResponse;

public interface PermissionService {
    List<PermissionResponse> getAll();

    List<Integer> getPermissionOfRole(Integer roleId);

    void updateRolePermission(
            Integer roleId,
            UpdateRolePermissionRequest request);
}
