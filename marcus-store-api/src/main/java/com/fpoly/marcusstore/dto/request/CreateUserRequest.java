package com.fpoly.marcusstore.dto.request;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;

    private String password;

    private String email;

    private String phoneNumber;

    private String fullName;
}
