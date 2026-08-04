package com.fpoly.marcusstore.dto.request;
import java.util.Set;

import com.fpoly.marcusstore.dto.response.PermissionResponse;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeePositionRequest {

    @NotBlank(message = "Tên chức danh không được để trống")
    private String positionName;

    private String description;

    private Set<PermissionResponse> permissions;
}