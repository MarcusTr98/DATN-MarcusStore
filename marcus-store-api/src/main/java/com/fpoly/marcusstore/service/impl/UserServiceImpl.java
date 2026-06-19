package com.fpoly.marcusstore.service.impl;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.dto.request.CreateUserRequest;
import com.fpoly.marcusstore.dto.request.UpdateUserRequest;
import com.fpoly.marcusstore.dto.response.UserResponse;
import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.service.UserService;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
private final UserRepository userRepository;
private final RoleRepository roleRepository;
private final PasswordEncoder passwordEncoder;

private UserResponse toResponse( User user){
    return UserResponse.builder()
          .userId(user.getUserId())
          .username(user.getUsername())
          .email(user.getEmail())
          .fullName(user.getFullName())
          .phoneNumber(user.getPhoneNumber())
          .active(user.getIsActive())
          .roleName(user.getRole().getRoleName())
          .emailVerified(user.getEmailVerified())
          .createdAt(user.getCreatedAt())
          .build();
}
@Override
@Transactional
public Page<UserResponse> getALL(String keyword, Pageable pageable){
          if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll(pageable)
                    .map(this::toResponse);
        }

        return userRepository
                .findByFullNameContainingIgnoreCase(
                        keyword,
                        pageable)
                .map(this::toResponse);
}
@Override
@Transactional
public UserResponse getById(Integer id){
     User user = userRepository.findById(id).orElseThrow(()->
         new RuntimeException("Không tìm thấy user với Id" + id));
    return toResponse(user);
}
@Override
@Transactional
public UserResponse create(CreateUserRequest request){
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new RuntimeException("Tên đã tồn tại");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new RuntimeException("Email đã tồn tại");
    }
    Role role = roleRepository.findByRoleName("STAFF").orElseThrow(()-> new RuntimeException("Không tìm thấy role Staff"));
         User user = new User();
         user.setUsername(request.getUsername());
         user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
         user.setFullName(request.getFullName());
         user.setEmail(request.getEmail());
         user.setPhoneNumber(request.getPhoneNumber());
         user.setIsActive(true);
         user.setRole(role);
         user.setCreatedAt(LocalDateTime.now());
         return toResponse(userRepository.save(user));
}
@Override
@Transactional
public UserResponse update(Integer Id, UpdateUserRequest request){
    User user = userRepository.findById(Id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy user"));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new RuntimeException("Email đã tồn tại");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy role"));
         user.setFullName(request.getFullName());
         user.setEmail(request.getEmail());
         user.setPhoneNumber(request.getPhoneNumber());
         user.setIsActive(true);
         user.setRole(role);
         user.setUpdatedAt(LocalDateTime.now());
         
        return toResponse(userRepository.save(user));
}
@Override
@Transactional
public void lockUser(Integer Id) {

    User user = userRepository.findById(Id)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy user với id: " + Id));

    if ("ADMIN".equals(user.getRole().getRoleName())) {
        throw new RuntimeException("Không thể khóa tài khoản Admin");
    }

    if (!Boolean.TRUE.equals(user.getIsActive())) {
        throw new RuntimeException("Tài khoản đã bị khóa");
    }

    user.setIsActive(false);

    userRepository.save(user);
}
@Override
@Transactional
public void UnLockUser(Integer Id) {

    User user = userRepository.findById(Id)
            .orElseThrow(() ->
                    new RuntimeException("Không tìm thấy user với id: " + Id));

    if (Boolean.TRUE.equals(user.getIsActive())) {
        throw new RuntimeException("Tài khoản đang hoạt động");
    }

    user.setIsActive(true);

    userRepository.save(user);
}
}
