package com.fpoly.marcusstore.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.fpoly.marcusstore.dto.request.UpdateUserPermissionRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PermissionResponse;
import com.fpoly.marcusstore.service.PermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/staff-permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAll() {

        return ApiResponse.success(permissionService.getAll()

        );

    }

    @GetMapping("/{userId}")
    public ApiResponse<List<Integer>> getPermissionOfUser(
            @PathVariable Integer userId) {

        return ApiResponse.success(
                permissionService.getPermissionOfUser(userId)
        );
    }

    @PutMapping("/{userId}")
    public ApiResponse<String> update(
            @PathVariable Integer userId,
            @RequestBody UpdateUserPermissionRequest request) {

        permissionService.updateUserPermission(userId, request);

        return ApiResponse.success("Cập nhật thành công");
    }
}
