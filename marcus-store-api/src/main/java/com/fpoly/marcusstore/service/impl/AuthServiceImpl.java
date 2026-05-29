package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.LoginRequest;
import com.fpoly.marcusstore.dto.response.JwtResponse;
import com.fpoly.marcusstore.service.AuthService;

import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {

        return null;
    }
}