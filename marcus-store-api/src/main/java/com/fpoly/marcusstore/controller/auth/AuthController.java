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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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

        public ResponseEntity<?> authenticateUser(
                        @Valid @RequestBody LoginRequest loginRequest) {

                try {

                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        String jwt = jwtUtils.generateJwtToken(authentication);

                        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                        List<String> roles = userDetails.getAuthorities()
                                        .stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .collect(Collectors.toList());

                        JwtResponse jwtResponse = new JwtResponse(
                                        jwt,
                                        userDetails.getUserId(),
                                        userDetails.getUsername(),
                                        userDetails.getEmail(),
                                        roles);

                        return ResponseEntity.ok(
                                        ApiResponse.success(jwtResponse));

                } catch (BadCredentialsException e) {

            return ResponseEntity.status(401)
                    .body(ApiResponse.error(
                            401,
                            "Sai tên đăng nhập hoặc mật khẩu"));

        } catch (DisabledException e) {

            return ResponseEntity.status(403)
                    .body(ApiResponse.error(
                            403,
                            "Tài khoản đã bị khóa"));

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body(ApiResponse.error(
                            500,
                            "Đã xảy ra lỗi hệ thống"));
        }
        }
}