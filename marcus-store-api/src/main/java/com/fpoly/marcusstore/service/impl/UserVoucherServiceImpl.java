package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.entity.shopping.UserVoucher;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.EmailService;
import com.fpoly.marcusstore.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserVoucherServiceImpl implements UserVoucherService {

    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // lấy voucher có thể sử dụng cho user
    @Override
    public List<VoucherResponse> getAvailableVouchersForUser() {
        Integer userId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        Map<Integer, VoucherResponse> resultByVoucherId = new LinkedHashMap<>();

        // 1. Lấy voucher ALL (ai cũng dùng được) — loại trừ voucher đã dùng
        List<Voucher> allVouchers = voucherRepository.findAvailableVouchers();
        for (Voucher v : allVouchers) {
            // Kiểm tra user đã dùng voucher này chưa
            boolean alreadyUsed = userVoucherRepository
                    .findByVoucherVoucherIdAndUserUserId(v.getVoucherId(), userId)
                    .map(UserVoucher::getIsUsed)
                    .orElse(false);
            if (!alreadyUsed) {
                resultByVoucherId.put(v.getVoucherId(), toResponseFromVoucher(v, false));
            }
        }

        // 2. Lấy voucher SPECIFIC đã gán cho user và chưa dùng
        List<UserVoucher> userVouchers = userVoucherRepository.findAvailableVouchersByUserId(userId, now);
        for (UserVoucher uv : userVouchers) {
            resultByVoucherId.putIfAbsent(uv.getVoucher().getVoucherId(), toResponse(uv));
        }

        return new ArrayList<>(resultByVoucherId.values());
    }

    @Override
    public List<VoucherResponse> getUserVouchersByVoucherId(Integer voucherId) {
        return userVoucherRepository.findByVoucherVoucherId(voucherId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // thêm voucher cho từng khách hàng
    @Override
    @Transactional
    public void assignVoucherToUsers(Integer voucherId, List<Integer> userIds) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        List<UserVoucher> userVouchers = new ArrayList<>();
        List<User> usersToNotify = new ArrayList<>();
        for (Integer userId : userIds) {
            if (!userVoucherRepository.existsByVoucherVoucherIdAndUserUserId(voucherId, userId)) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + userId));

                UserVoucher userVoucher = UserVoucher.builder()
                        .voucher(voucher)
                        .user(user)
                        .assignedAt(LocalDateTime.now())
                        .isUsed(false)
                        .build();

                userVouchers.add(userVoucher);
                usersToNotify.add(user);
            }
        }

        if (!userVouchers.isEmpty()) {
            userVoucherRepository.saveAll(userVouchers);

            // gửi mail sau khi save thành công
            for (User user : usersToNotify) {
                try {
                    emailService.sendVoucherAssigned(user.getEmail(), user.getFullName(), voucher);
                } catch (Exception e) {
                    // không để lỗi gửi mail làm rollback cả transaction gán voucher
                    // có thể log lại để theo dõi
                    // log.error("Gửi mail voucher thất bại cho user {}: {}", user.getUserId(),
                    // e.getMessage());
                }
            }
        }
    }

    @Override
    @Transactional
    public void reassignVoucherUsers(Integer voucherId, List<Integer> newUserIds) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        List<UserVoucher> currentUserVouchers = userVoucherRepository.findByVoucherVoucherId(voucherId);
        List<Integer> currentUserIds = currentUserVouchers.stream()
                .map(uv -> uv.getUser().getUserId())
                .collect(Collectors.toList());

        // Xóa user không còn trong danh sách mới
        for (Integer userId : currentUserIds) {
            if (!newUserIds.contains(userId)) {
                userVoucherRepository.findByVoucherVoucherIdAndUserUserId(voucherId, userId)
                        .ifPresent(userVoucherRepository::delete);
            }
        }

        // Thêm user mới
        for (Integer userId : newUserIds) {
            if (!currentUserIds.contains(userId)) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user với id: " + userId));

                UserVoucher userVoucher = UserVoucher.builder()
                        .voucher(voucher)
                        .user(user)
                        .assignedAt(LocalDateTime.now())
                        .isUsed(false)
                        .build();
                userVoucherRepository.save(userVoucher);
            }
        }
    }

    private VoucherResponse toResponse(UserVoucher userVoucher) {
        Voucher voucher = userVoucher.getVoucher();
        return VoucherResponse.builder()
                .userVoucherId(userVoucher.getId())
                .voucherId(voucher.getVoucherId())
                .voucherCode(voucher.getVoucherCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .isUsed(userVoucher.getIsUsed())
                .usedAt(userVoucher.getUsedAt())
                .assignedAt(userVoucher.getAssignedAt())
                .isActive(voucher.getIsActive())
                .build();
    }

    private VoucherResponse toResponseFromVoucher(Voucher voucher, boolean isUsed) {
        return VoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .voucherCode(voucher.getVoucherCode())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .isUsed(isUsed)
                .isActive(voucher.getIsActive())

                .build();
    }
}
