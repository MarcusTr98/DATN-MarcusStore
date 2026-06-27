package com.fpoly.marcusstore.controller.admin;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.fpoly.marcusstore.dto.request.CreateUserRequest;
import com.fpoly.marcusstore.dto.request.UpdateUserRequest;
import com.fpoly.marcusstore.dto.request.VerifyOtpRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;

import com.fpoly.marcusstore.dto.response.UserResponse;
import com.fpoly.marcusstore.service.impl.UserServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/user")
@PreAuthorize("hasAuthority('USER_VIEW')")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserServiceImpl userServiceImpl;
@GetMapping
  public ApiResponse<Page<UserResponse>> getAll(
          @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(userServiceImpl.getALL(keyword, roles, pageable));
}
 @PostMapping
@PreAuthorize("hasAuthority('USER_MANAGE')")
 public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request){
    return ApiResponse.success(userServiceImpl.create(request));
 }
@GetMapping("/{id}")
public ApiResponse<UserResponse> getById(@PathVariable("id") Integer id){
    return ApiResponse.success(userServiceImpl.getById(id));
}
@PutMapping("/{id}")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public ApiResponse<UserResponse> update( @PathVariable Integer id,@Valid @RequestBody UpdateUserRequest request){
    return ApiResponse.success(userServiceImpl.update(id, request));
}
@PutMapping("/{id}/lock")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public ApiResponse<String> lockUser(@PathVariable Integer id){
    userServiceImpl.lockUser(id);
    return ApiResponse.success("Khóa tài khoản thành công");
}

@PutMapping("/{id}/unLock")
@PreAuthorize("hasAuthority('USER_MANAGE')")
public ApiResponse<String> unLockUser(@PathVariable Integer id){
    userServiceImpl.UnLockUser(id);
    return ApiResponse.success("Mở khóa tài khoản thành công");
}
    @PostMapping("/{id}/send-verify-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> sendVerifyEmail(@PathVariable Integer id) {
        userServiceImpl.sendVerifyEmail(id);
        return ApiResponse.success("Email xác thực đã được gửi đến khách hàng");
    }
 
    // Khách submit OTP (không cần ADMIN)
    @PostMapping("/verify-email")
    @PreAuthorize("permitAll()")
    public ApiResponse<String> verifyEmail(@RequestBody @Valid VerifyOtpRequest request) {
        userServiceImpl.verifyEmailByOtp(request.getEmail(), request.getOtp());
        return ApiResponse.success("Xác thực email thành công");
    }
}
