package com.fpoly.marcusstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private List<String> roles;
    private List<String> permissions;
    public JwtResponse(String token, Integer id, String username, String email, String fullName, List<String> roles, List<String> permissions) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
        this.permissions = permissions;
    }
}