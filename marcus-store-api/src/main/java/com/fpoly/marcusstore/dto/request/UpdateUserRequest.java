package com.fpoly.marcusstore.dto.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String email;

    private String phoneNumber;

    private String fullName;

    private Integer roleId;
}
