package com.fpoly.marcusstore.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.fpoly.marcusstore.dto.request.CreateUserRequest;
import com.fpoly.marcusstore.dto.request.UpdateUserRequest;
import com.fpoly.marcusstore.dto.response.UserResponse;

public interface UserService {
   Page<UserResponse> getALL(String keyWord, Pageable page);
   UserResponse getById(Integer Id);
   UserResponse create(CreateUserRequest request);
   UserResponse update(Integer Id,UpdateUserRequest request);
   void lockUser(Integer Id);
   void UnLockUser(Integer Id);
   void sendVerifyEmail(Integer userId);
   void verifyEmailByOtp(String email, String otp);
}
