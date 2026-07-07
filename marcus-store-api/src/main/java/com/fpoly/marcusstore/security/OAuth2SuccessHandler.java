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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private static final int MAX_USERNAME_RETRY = 5;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String socialId;

        if ("google".equals(provider)) {
            socialId = oauthUser.getAttribute("sub");
        } else if ("facebook".equals(provider)) {
            socialId = oauthUser.getAttribute("id");
        } else {
            response.sendRedirect(frontendUrl + "/auth/login?error=unsupported_provider");
            return;
        }

        String email = oauthUser.getAttribute("email");
        String fullName = oauthUser.getAttribute("name");

        // Facebook có thể không trả email
        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/auth/login?error=no_email");
            return;
        }

        User user = null;

        // Tìm theo Social ID (đã JOIN FETCH role + permissions)
        if (socialId != null) {
            user = userRepository.findByGoogleAccountId(socialId).orElse(null);
        }

        // Nếu chưa liên kết thì tìm theo email (đã JOIN FETCH role + permissions)
        if (user == null) {
            user = userRepository.findByEmailWithRole(email).orElse(null);
        }

        // ======================
        // Chưa có tài khoản
        // ======================
        if (user == null) {

            // Fetch kèm permissions luôn để tránh lazy-load sau này
            Role customerRole = roleRepository.findByRoleNameWithPermissions("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role CUSTOMER"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setPasswordHash(null);
            newUser.setGoogleAccountId(socialId);
            newUser.setRole(customerRole);
            newUser.setIsActive(true);
            newUser.setEmailVerified(true);

            user = saveWithUniqueUsername(newUser, email);
            // Không cần requery: role + permissions đã có sẵn trong memory (fetch ở trên)
        }

        // ======================
        // Đã có tài khoản
        // ======================
        else {

            // Chỉ CUSTOMER được đăng nhập Social
            if (!"CUSTOMER".equalsIgnoreCase(user.getRole().getRoleName())) {

                response.sendRedirect(
                        frontendUrl + "/auth/login?error=social_not_allowed");

                return;
            }

            // Tài khoản bị khóa
            if (!Boolean.TRUE.equals(user.getIsActive())) {

                response.sendRedirect(
                        frontendUrl + "/auth/login?error=account_disabled");

                return;
            }

            // Chưa liên kết Social
            if (user.getGoogleAccountId() == null) {

                user.setGoogleAccountId(socialId);
                user = userRepository.save(user);
                // Không cần requery: user gốc đã JOIN FETCH role/permissions,
                // save() chỉ update, association trong memory vẫn còn nguyên
            }
        }

        // ======================
        // Sinh JWT
        // ======================

        CustomUserDetails userDetails = CustomUserDetails.build(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
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

        response.sendRedirect(
                frontendUrl + "/oauth-success"
                        + "?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                        + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                        + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                        + "&roles=" + URLEncoder.encode(roles, StandardCharsets.UTF_8)
                        + "&permissions=" + URLEncoder.encode(permissions, StandardCharsets.UTF_8));
    }

    /**
     * Lưu user mới với username random, tự retry nếu đụng unique constraint.
     * Không query trước để check tồn tại — chỉ retry khi INSERT thật sự conflict.
     */
    private User saveWithUniqueUsername(User newUser, String email) {

        int attempts = 0;

        while (true) {
            newUser.setUsername(generateRandomUsername(email));

            try {
                return userRepository.save(newUser);
            } catch (DataIntegrityViolationException e) {
                attempts++;
                if (attempts >= MAX_USERNAME_RETRY) {
                    throw new RuntimeException(
                            "Không thể tạo username duy nhất sau " + MAX_USERNAME_RETRY + " lần thử", e);
                }
                // random lại username, thử insert tiếp
            }
        }
    }

    /**
     * Sinh username ngẫu nhiên, KHÔNG query DB để check trùng trước.
     * Xác suất trùng gần như bằng 0 với 6 ký tự hex random.
     */
    private String generateRandomUsername(String email) {

        String prefix = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "");

        if (prefix.length() < 3) {
            prefix = "user";
        }

        String randomSuffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6);

        return prefix + "_" + randomSuffix;
    }
}