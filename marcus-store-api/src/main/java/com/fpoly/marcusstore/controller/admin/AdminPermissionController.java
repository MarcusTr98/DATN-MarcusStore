package com.fpoly.marcusstore.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.fpoly.marcusstore.dto.request.UpdateRolePermissionRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.PermissionResponse;
import com.fpoly.marcusstore.service.PermissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPermissionController {
    private final PermissionService permissionService;

    @GetMapping
    public ApiResponse<List<PermissionResponse>> getAll() {

        return ApiResponse.success(permissionService.getAll()

        );

    }

    @GetMapping("/role/{roleId}")

    public ApiResponse<List<Integer>> getPermissionOfRole(@PathVariable Integer roleId) {

        return ApiResponse.success(permissionService.getPermissionOfRole(roleId));

    }

    @PutMapping("/role/{roleId}")

    public ApiResponse<String> update(@PathVariable Integer roleId, @RequestBody UpdateRolePermissionRequest request) {
          permissionService.updateRolePermission(roleId,request);

        return ApiResponse.success("OK");

    }
}
