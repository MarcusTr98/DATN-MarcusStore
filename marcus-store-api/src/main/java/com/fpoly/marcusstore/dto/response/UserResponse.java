package com.fpoly.marcusstore.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    
    private Integer userId;

    private String username;

    private String email;

    private String phoneNumber;

    private String fullName;

    private Boolean active;

    private Boolean emailVerified;

    private String roleName;

    private LocalDateTime createdAt;

}
