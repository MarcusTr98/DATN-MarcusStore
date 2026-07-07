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

        // Fallback nếu provider không trả về tên
        if (fullName == null || fullName.isBlank()) {
            fullName = email.split("@")[0];
        }

        // Chỉ Google mới cần check email_verified
        // (Facebook đã đảm bảo email trả về là verified theo chính sách của họ)
        if ("google".equals(provider)) {
            Object emailVerifiedAttr = oauthUser.getAttribute("email_verified");
            boolean emailVerified = Boolean.TRUE.equals(emailVerifiedAttr)
                    || "true".equalsIgnoreCase(String.valueOf(emailVerifiedAttr));

            if (!emailVerified) {
                response.sendRedirect(frontendUrl + "/auth/login?error=email_not_verified");
                return;
            }
        }

        User user = null;
        boolean isNewlyLinkedToExistingAccount = false;

        // Tìm theo Social ID tương ứng provider (đã JOIN FETCH role + permissions)
        if ("google".equals(provider)) {
            user = userRepository.findByGoogleAccountId(socialId).orElse(null);
        } else if ("facebook".equals(provider)) {
            user = userRepository.findByFacebookAccountId(socialId).orElse(null);
        }

        // Nếu chưa liên kết thì tìm theo email
        if (user == null) {
            user = userRepository.findByEmailWithRole(email).orElse(null);
            if (user != null) {
                isNewlyLinkedToExistingAccount = true;
            }
        }

        // ======================
        // Chưa có tài khoản -> tạo mới
        // ======================
        if (user == null) {

            Role customerRole = roleRepository.findByRoleNameWithPermissions("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy role CUSTOMER"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(fullName);
            newUser.setPasswordHash(null);
            newUser.setRole(customerRole);
            newUser.setIsActive(true);
            newUser.setEmailVerified(true);

            if ("google".equals(provider)) {
                newUser.setGoogleAccountId(socialId);
            } else {
                newUser.setFacebookAccountId(socialId);
            }

            User savedUser = saveOrRecoverFromRaceCondition(newUser, email);

            // Nếu do race condition mà kết quả trả về không phải chính newUser
            // (một request khác đã tạo user với cùng email trước đó)
            // -> cần liên kết social id vào user thật sự đó
            if (savedUser != newUser) {
                isNewlyLinkedToExistingAccount = true;
                user = linkSocialIdIfMissing(savedUser, provider, socialId);
            } else {
                user = savedUser;
            }
        }

        // ======================
        // Đã có tài khoản (tìm theo social id hoặc theo email)
        // ======================
        else {

            if (!"CUSTOMER".equalsIgnoreCase(user.getRole().getRoleName())) {
                response.sendRedirect(frontendUrl + "/auth/login?error=social_not_allowed");
                return;
            }

            if (!Boolean.TRUE.equals(user.getIsActive())) {
                response.sendRedirect(frontendUrl + "/auth/login?error=account_disabled");
                return;
            }

            user = linkSocialIdIfMissing(user, provider, socialId);
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

        String redirectUrl = frontendUrl + "/oauth-success"
                + "?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(user.getUsername(), StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                + "&roles=" + URLEncoder.encode(roles, StandardCharsets.UTF_8)
                + "&permissions=" + URLEncoder.encode(permissions, StandardCharsets.UTF_8);

        if (isNewlyLinkedToExistingAccount) {
            redirectUrl += "&notice=account_linked";
        }

        response.sendRedirect(redirectUrl);
    }

    /**
     * Gán social id vào user nếu chưa có, theo đúng field từng provider.
     */
    private User linkSocialIdIfMissing(User user, String provider, String socialId) {

        boolean needsSave = false;

        if ("google".equals(provider) && user.getGoogleAccountId() == null) {
            user.setGoogleAccountId(socialId);
            needsSave = true;
        } else if ("facebook".equals(provider) && user.getFacebookAccountId() == null) {
            user.setFacebookAccountId(socialId);
            needsSave = true;
        }

        if (needsSave) {
            return userRepository.save(user);
        }

        return user;
    }

    /**
     * Lưu user mới với username random, tự retry nếu đụng unique constraint.
     * Nếu conflict là do race condition trên email (2 request cùng lúc tạo
     * user mới với cùng email), tự động phát hiện và trả về user đã được
     * request kia tạo trước đó, thay vì thất bại oan.
     */
    private User saveOrRecoverFromRaceCondition(User newUser, String email) {

        int attempts = 0;

        while (true) {
            newUser.setUsername(generateRandomUsername(email));

            try {
                return userRepository.save(newUser);
            } catch (DataIntegrityViolationException e) {

                User existing = userRepository.findByEmailWithRole(email).orElse(null);
                if (existing != null) {
                    return existing;
                }

                attempts++;
                if (attempts >= MAX_USERNAME_RETRY) {
                    throw new RuntimeException(
                            "Không thể tạo tài khoản sau " + MAX_USERNAME_RETRY + " lần thử", e);
                }
            }
        }
    }

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