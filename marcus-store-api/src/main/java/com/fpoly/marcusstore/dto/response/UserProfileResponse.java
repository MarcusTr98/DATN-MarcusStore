package com.fpoly.marcusstore.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
}