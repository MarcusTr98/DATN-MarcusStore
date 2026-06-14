package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.ProfileRequestDTO;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ProfileResponseDTO;
import com.fpoly.marcusstore.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
public class AdminProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ApiResponse<ProfileResponseDTO> getAdminProfile() {
        return ApiResponse.success(userProfileService.getMyProfile());
    }

    @PutMapping
    public ApiResponse<ProfileResponseDTO> updateAdminProfile(@Valid @RequestBody ProfileRequestDTO request) {
        return ApiResponse.success(userProfileService.updateProfile(request));
    }
}