package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.request.ApplyVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherApplyResult;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.dto.response.VoucherUsageResponse;
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
import java.math.RoundingMode;
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

    // convert từ entity sang DTO
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

        Page<Voucher> pageResult = voucherRepository
                .searchVouchers(normalizedKeyword, normalizedDiscountType, isActive, pageable);

        // FE sẽ tự xử lý logic lùi trang khi currentPage vượt quá totalPages
        // (không fallback ở BE nữa để tránh nhầm lẫn giữa 2 tầng xử lý)
        return pageResult.map(this::toResponse);
    }

    // lấy danh sách thống kê voucher
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

    // Lấy chi tiết voucher theo ID
    @Override
    public VoucherResponse getVoucherById(Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        return toResponse(voucher);
    }


    // thêm mới voucher
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

        // Validate quantity:
        // - ALL: bắt buộc phải có và > 0 (admin nhập tay, là tổng lượt dùng)
        // - SPECIFIC: không cần nhập, tự sinh = số user được chọn (validate sau ở buildVoucher)
        if ("ALL".equals(targetType)) {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Voucher áp dụng cho tất cả phải có số lượng lớn hơn 0"
                );
            }
        }

        Voucher voucher = buildVoucher(request, voucherCode, discountType);
        Voucher savedVoucher = voucherRepository.saveAndFlush(voucher);

        if ("SPECIFIC".equals(targetType) && request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
            userVoucherService.assignVoucherToUsers(savedVoucher.getVoucherId(), request.getTargetUserIds());
        }

        return toResponse(savedVoucher);
    }

    // Tao doi tuong Voucher tu request
    private Voucher buildVoucher(AddVoucherRequest request, String voucherCode, String discountType) {
        Voucher voucher = new Voucher();
        voucher.setVoucherCode(voucherCode);
        voucher.setDiscountType(discountType);
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());


        // Set targetType - mặc định là ALL
        voucher.setTargetType(request.getTargetType() != null ? request.getTargetType() : "ALL");

        // Tính quantity theo targetType:
        // - SPECIFIC: tự động = số user được chọn (mỗi user 1 lượt)
        // - ALL: lấy từ request, mặc định 1
        Integer resolvedQuantity;
        if ("SPECIFIC".equalsIgnoreCase(voucher.getTargetType())
                && request.getTargetUserIds() != null
                && !request.getTargetUserIds().isEmpty()) {
            resolvedQuantity = request.getTargetUserIds().size();
        } else {
            resolvedQuantity = request.getQuantity() != null ? request.getQuantity() : 1;
        }

        switch (discountType) {
            case "PERCENT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
                voucher.setQuantity(resolvedQuantity);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(resolvedQuantity);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(resolvedQuantity);
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

    // kiểm tra loại giảm giá hợp lệ
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

    // validate các trường giảm giá
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

    // validate ngày khi nhập voucher
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

    // cập nhật thông tin voucher
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

        // Cập nhật quantity theo targetType mới:
        // - SPECIFIC: tự động = số user được chọn (đã đồng bộ với UserVoucher ở trên)
        // - ALL: lấy từ request, giữ nguyên nếu null
        Integer resolvedQuantity;
        if ("SPECIFIC".equals(targetType) && newTargetUserIds != null && !newTargetUserIds.isEmpty()) {
            resolvedQuantity = newTargetUserIds.size();
        } else {
            resolvedQuantity = request.getQuantity() != null ? request.getQuantity() : voucher.getQuantity();
        }

        // Cập nhật discountValue và maxDiscountAmount theo discountType
        switch (discountType) {
            case "PERCENT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
                voucher.setQuantity(resolvedQuantity);
                break;
            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(resolvedQuantity);
                break;
            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(resolvedQuantity);
                break;
        }

        return toResponse(voucherRepository.save(voucher));
    }

    // ngừng hoạt động (soft-delete) voucher theo ID - chỉ cần ID, không cần body
    @Override
    @Transactional
    public void deleteVoucher(Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy voucher"
                ));

        voucher.setIsActive(false);
        voucherRepository.save(voucher);
    }

    // Helper: tự động deactivate voucher nếu hết quantity hoặc quá hạn
    private boolean deactivateIfExpired(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        // 1. Hết quantity → tắt
        if (voucher.getQuantity() != null && voucher.getQuantity() <= 0
                && Boolean.TRUE.equals(voucher.getIsActive())) {
            voucher.setIsActive(false);
            changed = true;
        }

        // 2. Quá hạn endDate → tắt
        if (voucher.getEndDate() != null
                && voucher.getEndDate().isBefore(now)
                && Boolean.TRUE.equals(voucher.getIsActive())) {
            voucher.setIsActive(false);
            changed = true;
        }

        if (changed) {
            voucherRepository.save(voucher);
        }
        return changed;
    }

    // áp dụng voucher vào đơn hàng
    @Override
    @Transactional
    public VoucherApplyResult applyVoucher(ApplyVoucherRequest request, Integer userId) {
        // 1. Tìm voucher theo code
        Voucher voucher = voucherRepository.findByVoucherCode(request.getVoucherCode().trim().toUpperCase())
                .orElse(null);

        if (voucher == null) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Mã giảm giá không tồn tại.")
                    .build();
        }

        // 2. Auto deactivate nếu quá hạn hoặc hết quantity (lazy deactivate)
        deactivateIfExpired(voucher);

        // 3. Validate isActive
        if (Boolean.FALSE.equals(voucher.getIsActive())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Voucher đã hết hạn hoặc hết lượt sử dụng, vui lòng chọn voucher khác.")
                    .build();
        }

        // 4. Validate ngày hiệu lực
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Mã giảm giá chưa có hiệu lực.")
                    .build();
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Voucher đã hết hạn, vui lòng chọn voucher khác.")
                    .build();
        }

        // Validate đơn tối thiểu
        if (voucher.getMinOrderValue() != null &&
                request.getOrderAmount().compareTo(voucher.getMinOrderValue()) < 0) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Đơn hàng chưa đạt giá trị tối thiểu để sử dụng mã này.")
                    .build();
        }

        // 3. Kiểm tra user có được phép dùng voucher này không
        boolean isAllUsers = !"SPECIFIC".equals(voucher.getTargetType());

        if (!isAllUsers) {
            boolean isAssigned = userVoucherRepository.existsByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), userId);
            if (!isAssigned) {
                return VoucherApplyResult.builder()
                        .applied(false)
                        .message("Bạn không được phép sử dụng mã giảm giá này.")
                        .build();
            }
        }

        // 4. Kiểm tra user đã dùng voucher này chưa
        Optional<UserVoucher> existingUsage = userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), userId);

        if (existingUsage.isPresent()) {
            if (Boolean.TRUE.equals(existingUsage.get().getIsUsed())) {
                return VoucherApplyResult.builder()
                        .applied(false)
                        .message("Bạn đã sử dụng mã giảm giá này rồi.")
                        .build();
            }
            // Đã được gán nhưng chưa dùng -> đánh dấu để confirm sau
            // KHÔNG save ngay, chờ confirmVoucherUsage()
        }
        // Voucher ALL: chưa có record -> sẽ tạo trong confirmVoucherUsage()

        // 5. Kiểm tra quota (chỉ đọc - trừ quantity sẽ xảy ra trong confirmVoucherUsage)
        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            if ("SPECIFIC".equals(voucher.getTargetType())) {
                // SPECIFIC: reset UserVoucher.isUsed về false nếu user đã được gán
                userVoucherRepository.findByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), userId)
                        .ifPresent(uv -> {
                            if (Boolean.TRUE.equals(uv.getIsUsed())) {
                                uv.setIsUsed(false);
                                uv.setUsedAt(null);
                                userVoucherRepository.save(uv);
                            }
                        });
            }
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Voucher đã hết lượt sử dụng, vui lòng chọn voucher khác.")
                    .build();
        }
        // KHÔNG trừ quantity ở đây - confirmVoucherUsage sẽ trừ atomic với UserVoucher

        // 6. Tính discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal freeshipAmount = BigDecimal.ZERO;

      // hiện tại chỉ hiển thị ở UI còn cơ chế trừ sẽ làm sau
        if ("FREESHIP".equalsIgnoreCase(voucher.getDiscountType())) {
            // Không tính freeshipAmount ở đây - frontend tự xử lý hiển thị
            // freeshipAmount = voucher.getDiscountValue();
        } else if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            BigDecimal percent = voucher.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            discountAmount = request.getOrderAmount().multiply(percent);

            if (voucher.getMaxDiscountAmount() != null
                    && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                    && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        } else if ("AMOUNT".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = voucher.getDiscountValue();
        }

        // Tránh trường hợp tiền giảm lớn hơn tiền hàng
        if (discountAmount.compareTo(request.getOrderAmount()) > 0) {
            discountAmount = request.getOrderAmount();
        }

        return VoucherApplyResult.builder()
                .voucherId(voucher.getVoucherId())
                .voucherCode(voucher.getVoucherCode())
                .discountType(voucher.getDiscountType())
                .discountAmount(discountAmount)
                .freeshipAmount(freeshipAmount)
                .applied(true)
                .message("Áp dụng mã giảm giá thành công.")
                .build();
    }

    // xác nhận voucher dduojc sử dụng nếu thành công
    @Override
    @Transactional
    public void confirmVoucherUsage(Integer voucherId, Integer userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm voucher
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));

        // 2. Nếu voucher đã bị deactivate trước đó → throw lỗi thân thiện
        if (Boolean.FALSE.equals(voucher.getIsActive())) {
            throw new RuntimeException("Voucher đã hết hạn hoặc hết lượt sử dụng, vui lòng chọn voucher khác.");
        }

        // 3. Nếu endDate đã qua → auto deactivate + throw lỗi
        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            voucher.setIsActive(false);
            voucherRepository.save(voucher);
            throw new RuntimeException("Voucher đã hết hạn, vui lòng chọn voucher khác.");
        }

        // 4. Nếu hết quantity → auto deactivate + throw lỗi (race condition guard)
        if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
            voucher.setIsActive(false);
            voucherRepository.save(voucher);
            throw new RuntimeException("Voucher đã hết lượt sử dụng, vui lòng chọn voucher khác.");
        }

        // 5. Trừ quantity
        voucher.setQuantity(voucher.getQuantity() - 1);

        // 6. chuyển trạng thái của voucher nểu số lượng <= 0
        if (voucher.getQuantity() <= 0) {
            voucher.setIsActive(false);
        }
        voucherRepository.save(voucher);

        // 7. Update hoặc create UserVoucher record
        Optional<UserVoucher> userVoucherOpt = userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucherId, userId);

        if (userVoucherOpt.isPresent()) {
            UserVoucher userVoucher = userVoucherOpt.get();
            if (!Boolean.TRUE.equals(userVoucher.getIsUsed())) {
                userVoucher.setIsUsed(true);
                userVoucher.setUsedAt(now);
                userVoucherRepository.save(userVoucher);
            }
        } else {
            // Voucher ALL: tạo UserVoucher record mới cho user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user."));
            UserVoucher newUserVoucher = UserVoucher.builder()
                    .voucher(voucher)
                    .user(user)
                    .assignedAt(now)
                    .isUsed(true)
                    .usedAt(now)
                    .build();
            userVoucherRepository.save(newUserVoucher);
        }
    }

    // Chuyen doi UserVoucher thanh VoucherUsageResponse
    private VoucherUsageResponse toUsageResponse(UserVoucher uv) {
        User user = uv.getUser();
        Voucher voucher = uv.getVoucher();

        String userFullName = null;
        if (user != null) {
            userFullName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        }

        return VoucherUsageResponse.builder()
                .voucherUsageId(uv.getId())
                .voucherId(voucher.getVoucherId())
                .voucherCode(voucher.getVoucherCode())
                .voucherDiscountType(voucher.getDiscountType())
                .voucherDiscountValue(voucher.getDiscountValue())
                .voucherMaxDiscount(voucher.getMaxDiscountAmount())
                .userId(user != null ? user.getUserId() : null)
                .userFullName(userFullName)
                .userEmail(user != null ? user.getEmail() : null)
                .usedAt(uv.getUsedAt())
                .build();
    }

    // get lịch sử voucher user đã dùng
    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> getVoucherUsageHistory(Integer voucherId) {
        List<UserVoucher> usages = userVoucherRepository.findUsedByVoucherId(voucherId);
        return usages.stream()
                .map(this::toUsageResponse)
                .collect(Collectors.toList());
    }

    // laasys lịch sử dùng của 1 user
    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> getUserVoucherUsageHistory(Integer userId) {
        List<UserVoucher> usages = userVoucherRepository.findUsedByUserId(userId);
        return usages.stream()
                .map(this::toUsageResponse)
                .collect(Collectors.toList());
    }

    // đếm số lần sử dụng của 1 user
    @Override
    @Transactional(readOnly = true)
    public long getVoucherUsedCount(Integer voucherId) {
        return userVoucherRepository.countUsedByVoucherId(voucherId);
    }
}
