package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.UserVoucher;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.promotion.UserVoucherRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;
import com.fpoly.marcusstore.service.UserVoucherService;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final UserRepository userRepository;
    private final UserVoucherService userVoucherService;

    private VoucherResponse toResponse(Voucher voucher) {
        // Lấy danh sách user đã được gán voucher nếu là SPECIFIC
        List<Integer> targetUserIds = null;
        Integer targetUserCount = null;

        if ("SPECIFIC".equals(voucher.getTargetType())) {
            List<UserVoucher> userVouchers = userVoucherRepository.findByVoucherVoucherId(voucher.getVoucherId());
            targetUserIds = userVouchers.stream()
                    .map(uv -> uv.getUser().getUserId())
                    .collect(Collectors.toList());
            targetUserCount = targetUserIds.size();
        }

        return VoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .voucherCode((voucher.getVoucherCode()))
                .discountValue(voucher.getDiscountValue())
                .discountType(voucher.getDiscountType())
                .maxDiscountAmount((voucher.getMaxDiscountAmount()))
                .minOrderValue(voucher.getMinOrderValue())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .quantity(voucher.getQuantity())
                .isActive(voucher.getIsActive())
                .targetType(voucher.getTargetType())
                .targetUserIds(targetUserIds)
                .targetUserCount(targetUserCount)
                .build();
    }

    @Override
    public Page<VoucherResponse> getVouchersPage(String keyword, String discountType, Boolean isActive, Pageable pageable) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedDiscountType = normalizeDiscountType(discountType);

        return voucherRepository
                .searchVouchers(normalizedKeyword, normalizedDiscountType, isActive, pageable)
                .map(this::toResponse);
    }

    @Override
    public VoucherStatsResponse getVoucherStats(String keyword, String discountType, Boolean isActive) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedDiscountType = normalizeDiscountType(discountType);

        return new VoucherStatsResponse(
                voucherRepository.countVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countActiveVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countPercentVouchers(normalizedKeyword, normalizedDiscountType, isActive),
                voucherRepository.countAmountVouchers(normalizedKeyword, normalizedDiscountType, isActive)
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
    }

    private String normalizeDiscountType(String discountType) {
        return discountType == null ||
                discountType.isBlank() ||
                "ALL".equalsIgnoreCase(discountType)
                ? null
                : discountType.trim().toUpperCase();
    }

    @Override
    public VoucherResponse getVoucherById(Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        return toResponse(voucher);
    }

    @Override
    public void deleteVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("không tìm thấy voucher hợp lệ để xóa" + id));
        voucherRepository.delete(voucher);
    }

    @Override
    @Transactional
    public VoucherResponse addVoucher(AddVoucherRequest request) {
        String voucherCode = request.getVoucherCode().trim().toUpperCase();
        String discountType = request.getDiscountType().trim().toUpperCase();

        // Kiểm tra loại voucher
        validateDiscountType(discountType);

        // FREESHIP không cần validate maxDiscountAmount
        if (!"FREESHIP".equals(discountType)) {
            validateDiscountFields(request, discountType);
        }

        if (voucherRepository.existsByVoucherCode(voucherCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã voucher đã tồn tại"
            );
        }

        validateDateRange(request.getStartDate(), request.getEndDate());

        // Validate targetType
        String targetType = request.getTargetType() != null ? request.getTargetType().toUpperCase() : "ALL";
        if (!"ALL".equals(targetType) && !"SPECIFIC".equals(targetType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đối tượng sử dụng không hợp lệ. Các loại hợp lệ: ALL, SPECIFIC"
            );
        }

        // Nếu là SPECIFIC thì phải có danh sách user
        if ("SPECIFIC".equals(targetType) &&
            (request.getTargetUserIds() == null || request.getTargetUserIds().isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ít nhất một khách hàng để gán voucher"
            );
        }

        Voucher voucher = buildVoucher(request, voucherCode, discountType);
        Voucher savedVoucher = voucherRepository.saveAndFlush(voucher);

        if ("SPECIFIC".equals(targetType) && request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
            userVoucherService.assignVoucherToUsers(savedVoucher.getVoucherId(), request.getTargetUserIds());
        }

        return toResponse(savedVoucher);
    }

    private Voucher buildVoucher(AddVoucherRequest request, String voucherCode, String discountType) {
        Voucher voucher = new Voucher();
        voucher.setVoucherCode(voucherCode);
        voucher.setDiscountType(discountType);
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());


        // Set targetType - mặc định là ALL
        voucher.setTargetType(request.getTargetType() != null ? request.getTargetType() : "ALL");

        switch (discountType) {
            case "PERCENT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
                // Mặc định quantity = 1 (mỗi user dùng 1 lần)
                voucher.setQuantity(1);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                // Mặc định quantity = 1 (mỗi user dùng 1 lần)
                voucher.setQuantity(1);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                // Mặc định quantity = 1 (mỗi user dùng 1 lần)
                voucher.setQuantity(1);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Loại giảm giá không hợp lệ"
                );
        }

        return voucher;
    }

    private void validateDiscountType(String discountType) {
        if (discountType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại giảm giá không được để trống"
            );
        }

        if (!"PERCENT".equals(discountType) &&
                !"AMOUNT".equals(discountType) &&
                !"FREESHIP".equals(discountType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại giảm giá không hợp lệ. Các loại hợp lệ: PERCENT, AMOUNT, FREESHIP"
            );
        }
    }

    private void validateDiscountFields(AddVoucherRequest request, String discountType) {
        if ("PERCENT".equals(discountType)) {
            if (request.getMaxDiscountAmount() == null ||
                    request.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số tiền giảm tối đa phải lớn hơn 0"
                );
            }
        }

        if (request.getDiscountValue() == null ||
                request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá trị giảm phải lớn hơn 0"
            );
        }

        if ("PERCENT".equals(discountType) &&
                request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giảm theo phần trăm không được vượt quá 100%"
            );
        }
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu và ngày kết thúc không được để trống"
            );
        }

        if (!endDate.isAfter(startDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải lớn hơn ngày bắt đầu"
            );
        }
    }

    @Override
    @Transactional
    public VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request) {
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy voucher"
                ));

        String voucherCode = request.getVoucherCode().trim().toUpperCase();
        String discountType = request.getDiscountType().trim().toUpperCase();

        // Kiểm tra loại voucher
        validateDiscountType(discountType);

        // FREESHIP không cần validate maxDiscountAmount
        if (!"FREESHIP".equals(discountType)) {
            validateDiscountFields(request, discountType);
        }

        // Kiểm tra trùng mã
        if (voucherRepository.existsByVoucherCode(voucherCode) &&
                !voucher.getVoucherCode().equalsIgnoreCase(voucherCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã voucher đã tồn tại"
            );
        }

        validateDateRange(request.getStartDate(), request.getEndDate());

        // Validate targetType
        String targetType = request.getTargetType() != null ? request.getTargetType().toUpperCase() : "ALL";
        if (!"ALL".equals(targetType) && !"SPECIFIC".equals(targetType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đối tượng sử dụng không hợp lệ. Các loại hợp lệ: ALL, SPECIFIC"
            );
        }

        // Cập nhật các trường voucher
        voucher.setVoucherCode(voucherCode);
        voucher.setDiscountType(discountType);
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());

        voucher.setTargetType(targetType);
        voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));

        // Cập nhật discountValue và maxDiscountAmount theo discountType
        switch (discountType) {
            case "PERCENT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
                voucher.setQuantity(1);
                break;
            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(1);
                break;
            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(1);
                break;
        }

        // Xử lý UserVoucher khi thay đổi targetType hoặc targetUserIds
        String oldTargetType = voucher.getTargetType();
        List<Integer> newTargetUserIds = request.getTargetUserIds();

        if ("SPECIFIC".equals(targetType) && newTargetUserIds != null && !newTargetUserIds.isEmpty()) {
            // SPECIFIC: Cần cập nhật danh sách user
            if ("SPECIFIC".equals(oldTargetType)) {
                // Chuyển từ SPECIFIC sang SPECIFIC khác -> reassign
                userVoucherService.reassignVoucherUsers(voucherId, newTargetUserIds);
            } else {
                // Chuyển từ ALL sang SPECIFIC -> gán mới
                userVoucherService.assignVoucherToUsers(voucherId, newTargetUserIds);
            }
        } else if ("ALL".equals(targetType)) {
            // Chuyển sang ALL: xóa hết UserVoucher cũ
            if ("SPECIFIC".equals(oldTargetType)) {
                List<UserVoucher> currentUserVouchers = userVoucherRepository.findByVoucherVoucherId(voucherId);
                for (UserVoucher uv : currentUserVouchers) {
                    userVoucherRepository.delete(uv);
                }
            }
        }

        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public List<VoucherResponse> getAvailableVouchers() {
        return voucherRepository.findAvailableVouchers()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // kiểm tra giá trije hợp lệ của voucher
    @Override
    @Transactional
    public boolean checkAndRecordVoucherUsage(Integer voucherId, Integer userId) {
        // 1. Tìm voucher
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

        // 2. Validate voucher
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new RuntimeException("Mã giảm giá không còn hoạt động.");
        }
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new RuntimeException("Mã giảm giá chưa có hiệu lực.");
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn.");
        }
        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
        }

        // 3. Kiểm tra user có được phép dùng voucher này không
        boolean isAllUsers = !"SPECIFIC".equals(voucher.getTargetType());

        if (!isAllUsers) {
            // Voucher "SPECIFIC": Kiểm tra user có trong danh sách được gán không
            boolean isAssigned = userVoucherRepository.existsByVoucherVoucherIdAndUserUserId(voucherId, userId);
            if (!isAssigned) {
                throw new RuntimeException("Bạn không được phép sử dụng mã giảm giá này.");
            }
        }

        // 4. Kiểm tra user đã dùng voucher này chưa
        Optional<UserVoucher> existingUsage = userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucherId, userId);

        if (existingUsage.isPresent()) {
            if (Boolean.TRUE.equals(existingUsage.get().getIsUsed())) {
                throw new RuntimeException("Bạn đã sử dụng mã giảm giá này rồi.");
            }
            // Đã được gán nhưng chưa dùng -> Update thành đã dùng
            UserVoucher userVoucher = existingUsage.get();
            userVoucher.setIsUsed(true);
            userVoucher.setUsedAt(now);
            userVoucherRepository.save(userVoucher);
        } else {
            // Voucher "ALL": Chưa có record nào -> Tạo mới
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user."));
            UserVoucher userVoucher = UserVoucher.builder()
                    .voucher(voucher)
                    .user(user)
                    .assignedAt(now)
                    .isUsed(true)
                    .usedAt(now)
                    .build();
            userVoucherRepository.save(userVoucher);
        }

        // 5. Trừ số lượng voucher
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucherRepository.save(voucher);

        return true;
    }
}
