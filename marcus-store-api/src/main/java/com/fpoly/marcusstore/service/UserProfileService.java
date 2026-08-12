package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProfileRequestDTO;
import com.fpoly.marcusstore.dto.response.MembershipTierResponseDTO;
import com.fpoly.marcusstore.dto.response.ProfileResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.OrderRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    // Ngưỡng hạng thành viên — đồng bộ với admin/UserServiceImpl.getMemberRank
    private static final BigDecimal SILVER_THRESHOLD  = new BigDecimal("50000000");
    private static final BigDecimal GOLD_THRESHOLD    = new BigDecimal("150000000");
    private static final BigDecimal DIAMOND_THRESHOLD = new BigDecimal("300000000");

    @Transactional(readOnly = true)
    public ProfileResponseDTO getMyProfile() {
        // 1. Lấy ID (Nếu lỗi ở đây, nó sẽ văng ra Exception báo tên SecurityUtils)
        Integer userId = SecurityUtils.getCurrentUserId();

        // 2. Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        // 3. Xử lý Role an toàn (Chống NullPointer)
        String roleName = "CUSTOMER";
        if (user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        // 4. Build DTO
        return ProfileResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roleName(roleName)
                .emailVerified(user.getEmailVerified())
                .build();
    }

    @Transactional
    public ProfileResponseDTO updateProfile(ProfileRequestDTO request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        String roleName = "CUSTOMER";
        if (user.getRole() != null) {
            roleName = user.getRole().getRoleName();
        }

        return ProfileResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roleName(roleName)
                 .emailVerified(user.getEmailVerified())
                .build();
    }

    /**
     * Hạng thành viên của khách đang đăng nhập.
     * Tính tổng chi tiêu (đơn COMPLETED) → suy ra hạng theo ngưỡng đồng bộ với admin.
     */
    @Transactional(readOnly = true)
    public MembershipTierResponseDTO getMyMembershipTier() {
        Integer userId = SecurityUtils.getCurrentUserId();

        BigDecimal totalSpent = orderRepository.sumTotalSpentByUserId(userId);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        String rank = resolveRank(totalSpent);

        return MembershipTierResponseDTO.builder()
                .rank(rank)
                .totalSpent(totalSpent)
                .build();
    }

    private String resolveRank(BigDecimal totalSpent) {
        if (totalSpent == null) return "Đồng";
        if (totalSpent.compareTo(DIAMOND_THRESHOLD) >= 0) return "Kim Cương";
        if (totalSpent.compareTo(GOLD_THRESHOLD)    >= 0) return "Vàng";
        if (totalSpent.compareTo(SILVER_THRESHOLD)  >= 0) return "Bạc";
        return "Đồng";
    }
}