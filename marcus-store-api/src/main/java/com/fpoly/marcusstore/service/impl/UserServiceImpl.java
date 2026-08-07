package com.fpoly.marcusstore.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.dto.request.CreateUserRequest;
import com.fpoly.marcusstore.dto.request.UpdateUserRequest;
import com.fpoly.marcusstore.dto.response.UserResponse;
import com.fpoly.marcusstore.entity.auth.EmailOTP;
import com.fpoly.marcusstore.entity.auth.Permission;
import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.EmailOTPRepository;
import com.fpoly.marcusstore.repository.auth.PermissionRepository;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.service.EmailService;
import com.fpoly.marcusstore.service.OtpService;
import com.fpoly.marcusstore.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final EmailOTPRepository emailOtpRepository;
    private final PermissionRepository permissionRepository;

    private UserResponse toResponse(User user) {
        BigDecimal totalSpent = BigDecimal.ZERO;
        if ("CUSTOMER".equals(user.getRole().getRoleName())) {
            totalSpent = orderRepository.sumTotalSpentByUserId(user.getUserId());
            if (totalSpent == null)
                totalSpent = BigDecimal.ZERO;
        }
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
        .totalSpent(totalSpent)
        .build();
    }

@Override
@Transactional
public Page<UserResponse> getALL(
        String keyword,
        List<String> roles,
        Boolean status,
        Boolean emailVerified,
        Pageable pageable) {

    String normalizedKeyword = normalizeKeyword(keyword);
    List<String> normalizedRoles = normalizeRoles(roles);

    boolean rolesEmpty = normalizedRoles.isEmpty();
    List<String> queryRoles =
            rolesEmpty ? List.of("__NO_ROLE__") : normalizedRoles;

    return userRepository.findAllByKeywordAndRoles(
            normalizedKeyword,
            queryRoles,
            rolesEmpty,
            status,
            emailVerified,
            pageable
    ).map(this::toResponse);
}

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .filter(role -> role != null)
                .flatMap(role -> Arrays.stream(role.split(",")))
                .filter(role -> !role.trim().isEmpty())
                .map(role -> role.trim().toUpperCase())
                .toList();
    }

    @Override
    @Transactional
    public UserResponse getById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với Id" + id));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setIsActive(true);
        user.setRole(role);
        // Gán quyền theo các module được chọn
if (request.getModuleNames() != null && !request.getModuleNames().isEmpty()) {

    List<Permission> permissions =
            permissionRepository.findByModuleNameIn(request.getModuleNames());

    user.setPermissions(new HashSet<>(permissions));
}
        user.setCreatedAt(LocalDateTime.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(Integer Id, UpdateUserRequest request) {
        User user = userRepository.findById(Id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new RuntimeException("Email đã tồn tại");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role"));
        if ("ADMIN".equals(user.getRole().getRoleName())
                && !"ADMIN".equals(role.getRoleName())) {
            throw new RuntimeException("Không thể thay đổi role của tài khoản Admin");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setIsActive(true);
        user.setRole(role);
        // Cập nhật lại quyền theo module
user.getPermissions().clear();

if (request.getModuleNames() != null && !request.getModuleNames().isEmpty()) {

    List<Permission> permissions =
            permissionRepository.findByModuleNameIn(request.getModuleNames());

    user.getPermissions().addAll(permissions);
}
        user.setUpdatedAt(LocalDateTime.now());

        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void lockUser(Integer Id) {

        User user = userRepository.findById(Id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + Id));

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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + Id));

        if (Boolean.TRUE.equals(user.getIsActive())) {
            throw new RuntimeException("Tài khoản đang hoạt động");
        }

        user.setIsActive(true);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void sendVerifyEmail(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email đã được xác thực");
        }

        // Xóa OTP cũ nếu có
        emailOtpRepository.deleteByEmail(user.getEmail());

        // Tạo OTP mới
        String otpCode = String.format("%06d", new java.util.Random().nextInt(999999));
        EmailOTP otp = new EmailOTP();
        otp.setEmail(user.getEmail());
        otp.setOtpCode(otpCode);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        emailOtpRepository.save(otp);

        // Gửi mail
        emailService.sendOtp(user.getEmail(), otpCode);
    }

    @Override
    @Transactional
    public void verifyEmailByOtp(String email, String otp) {

        System.out.println("VERIFY OTP: " + email);

        otpService.verifyOtp(email, otp);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        System.out.println("Before: " + user.getEmailVerified());

        user.setEmailVerified(true);

        userRepository.save(user);

        System.out.println("After: " + user.getEmailVerified());
    }

    private String getMemberRank(BigDecimal totalSpent) {

    if (totalSpent == null) {
        return "Đồng";
    }

    if (totalSpent.compareTo(new BigDecimal("300000000")) >= 0) {
        return "Kim Cương";
    }

    if (totalSpent.compareTo(new BigDecimal("150000000")) >= 0) {
        return "Vàng";
    }

    if (totalSpent.compareTo(new BigDecimal("50000000")) >= 0) {
        return "Bạc";
    }

    return "Đồng";
}
}