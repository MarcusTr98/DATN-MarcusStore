package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.LoginRequest;
import com.fpoly.marcusstore.dto.response.JwtResponse;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
}