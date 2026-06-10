package com.fpoly.marcusstore.controller.auth;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fpoly.marcusstore.dto.request.RegisterRequest;
import com.fpoly.marcusstore.dto.request.VerifyOtpRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.AuthRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRegisterController {


    private final AuthRegisterService authRegisterService;


     @PostMapping("/register/request")
    public ResponseEntity<?> requestRegister(
            @Valid @RequestBody RegisterRequest request
    ) {

        authRegisterService.requestRegister(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP đã được gửi tới email"
                )
        );
    }
    @PostMapping("/register/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyOtpRequest request) {

         String result = authRegisterService.verifyRegister(request);
    return ResponseEntity.ok(ApiResponse.success(result));
    }
}
