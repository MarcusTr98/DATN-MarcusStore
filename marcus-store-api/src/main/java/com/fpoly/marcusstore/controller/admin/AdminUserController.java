package com.fpoly.marcusstore.controller.admin;
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
import com.fpoly.marcusstore.dto.response.ApiResponse;

import com.fpoly.marcusstore.dto.response.UserResponse;
import com.fpoly.marcusstore.service.impl.UserServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminUserController {
    private final UserServiceImpl userServiceImpl;
@GetMapping
  public ApiResponse<Page<UserResponse>> getAll(
          @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(userServiceImpl.getALL(keyword, pageable));
}
 @PostMapping
 @PreAuthorize("hasRole('ADMIN')")
 public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request){
    return ApiResponse.success(userServiceImpl.create(request));
 }
@GetMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<UserResponse> getById(@PathVariable("id") Integer id){
    return ApiResponse.success(userServiceImpl.getById(id));
}
@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<UserResponse> update( @PathVariable Integer id,@Valid @RequestBody UpdateUserRequest request){
    return ApiResponse.success(userServiceImpl.update(id, request));
}
@PutMapping("/{id}/lock")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<String> lockUser(@PathVariable Integer id){
    userServiceImpl.lockUser(id);
    return ApiResponse.success("Khóa tài khoản thành công");
}
@PutMapping("/{id}/unLock")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<String> unLockUser(@PathVariable Integer id){
    userServiceImpl.UnLockUser(id);
    return ApiResponse.success("Mở khóa tài khoản thành công");
}
}
