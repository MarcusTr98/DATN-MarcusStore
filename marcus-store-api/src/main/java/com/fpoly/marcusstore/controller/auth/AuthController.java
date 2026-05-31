package com.fpoly.marcusstore.controller.auth;

import com.fpoly.marcusstore.dto.request.LoginRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.JwtResponse;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private JwtUtils jwtUtils;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(
                        @Valid @RequestBody LoginRequest loginRequest) {

                // 1. Xác thực tài khoản (Spring Security sẽ gọi CustomUserDetailsService kiểm
                // tra DB)
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                loginRequest.getPassword()));

                // 2. Nếu thành công, lưu thông tin vào Context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 3. Chế tạo mã Token (vé vào cửa)
                String jwt = jwtUtils.generateJwtToken(authentication);

                // 4. Lấy thông tin User để trả về Frontend
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                List<String> roles = userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toList());

                JwtResponse jwtResponse = new JwtResponse(
                                jwt,
                                userDetails.getUserId(),
                                userDetails.getUsername(),
                                userDetails.getEmail(),
                                roles);

                // 5. Trả về đúng định dạng ApiResponse chuẩn của dự án
                return ResponseEntity.ok(ApiResponse.success(jwtResponse));
        }
}