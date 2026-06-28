package com.fpoly.marcusstore.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fpoly.marcusstore.entity.auth.Permission;
import com.fpoly.marcusstore.entity.auth.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Integer userId;
    private String username;
    private String email;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;
    private Boolean isActive;
    public static CustomUserDetails build(User user) {
        // Lấy tên Role (VD: ADMIN, STAFF) ép thành Authority chuẩn của Spring
        List<GrantedAuthority> authorities = new ArrayList<>();
         String roleName = user.getRole().getRoleName();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName()));
                // Nếu là ADMIN thì luôn có SUPER_ADMIN
        if ("ADMIN".equalsIgnoreCase(roleName)) {
            authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
        }

        // Thêm toàn bộ permission
        if (user.getRole().getPermissions() != null) {
            for (Permission permission : user.getRole().getPermissions()) {
                authorities.add(
                        new SimpleGrantedAuthority(permission.getPermissionName())
                );
            }
        }
        return new CustomUserDetails(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities,
                user.getIsActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}