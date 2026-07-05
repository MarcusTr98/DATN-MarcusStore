package com.fpoly.marcusstore.security;

import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String googleId = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String fullName = oauthUser.getAttribute("name");

        User user = null;

        // Ưu tiên tìm theo Google ID
        if (googleId != null) {
            user = userRepository.findByGoogleAccountId(googleId).orElse(null);
        }

        // Nếu chưa liên kết thì tìm theo Email
        if (user == null) {
           user = userRepository.findByEmailWithRole(email)
        .orElse(null);
        }
        // Chưa có tài khoản -> tạo CUSTOMER
        if (user == null) {

            Role customerRole = roleRepository.findByRoleName("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role CUSTOMER"));

            user = new User();
            user.setUsername(generateUniqueUsername(email));
            user.setEmail(email);
            user.setFullName(fullName);

            // Nếu cột password_hash đang NOT NULL thì đổi thành ""
            user.setPasswordHash("");

            user.setGoogleAccountId(googleId);

            user.setRole(customerRole);

            user.setIsActive(true);

            user.setEmailVerified(true);

            user = userRepository.save(user);
        }
        // Đã có tài khoản
        else {

            // Không cho ADMIN / STAFF login Google
            if (!"CUSTOMER".equalsIgnoreCase(user.getRole().getRoleName())) {

                response.sendRedirect(
                        "http://localhost:5173/auth/login?error=google_not_allowed");

                return;
            }

            // Tài khoản bị khóa
            if (!Boolean.TRUE.equals(user.getIsActive())) {

                response.sendRedirect(
                        "http://localhost:5173/auth/login?error=account_disabled");

                return;
            }

            // Chưa liên kết Google thì cập nhật
            if (user.getGoogleAccountId() == null) {

                user.setGoogleAccountId(googleId);

                userRepository.save(user);
            }
        }

        // Sinh JWT
        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(user.getUsername());

        Authentication auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

        String jwt = jwtUtils.generateJwtToken(auth);

        String roles = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        String permissions = userDetails.getAuthorities()
                .stream()
                .map(a -> a.getAuthority())
                .filter(a -> !a.startsWith("ROLE_"))
                .reduce((a, b) -> a + "," + b)
                .orElse("");


        // Redirect về Vue
        response.sendRedirect(
                "http://localhost:5173/oauth-success"
                        + "?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                        + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                        + "&roles=" + URLEncoder.encode(roles, StandardCharsets.UTF_8)
                        + "&permissions=" + URLEncoder.encode(permissions, StandardCharsets.UTF_8)
        );
    }

    
     //Sinh username không trùng
    private String generateUniqueUsername(String email) {

        String username = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "");

        if (username.length() < 3) {
            username = "user";
        }

        String original = username;
        int index = 1;

        while (userRepository.existsByUsername(username)) {
            username = original + index++;
        }

        return username;
    }

}