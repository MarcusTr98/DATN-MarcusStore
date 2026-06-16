package com.fpoly.marcusstore.controller.auth;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fpoly.marcusstore.dto.request.ChangePasswordRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.service.ChangePasswordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/")
@RequiredArgsConstructor
public class ChangePasswordController {
     private final ChangePasswordService chagePass;

    @PutMapping("/change-password")
    public ApiResponse<String> changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest request) {

        chagePass.changePassword(request);

        return ApiResponse.success(
                "Đổi mật khẩu thành công");
    }
}
