package com.fpoly.marcusstore.service;

import java.util.List;

import com.fpoly.marcusstore.dto.request.UpdateUserPermissionRequest;
import com.fpoly.marcusstore.dto.response.PermissionResponse;

public interface PermissionService {
    // Lấy toàn bộ Permission để hiển thị checkbox
    List<PermissionResponse> getAll();

    // Lấy quyền của Staff
    List<Integer> getPermissionOfUser(Integer userId);

    // Cập nhật quyền Staff
    void updateUserPermission(
            Integer userId,
            UpdateUserPermissionRequest request);
}
