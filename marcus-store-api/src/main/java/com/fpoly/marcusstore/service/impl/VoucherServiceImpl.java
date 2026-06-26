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

    // Chuyen doi Entity thanh Response (co them thong tin user duoc gan neu la SPECIFIC)
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

    // Lay danh sach thong ke voucher (tong so, dang su dung, theo loai)
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

    // Lay thong tin chi tiet 1 voucher theo id
    @Override
    public VoucherResponse getVoucherById(Integer voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher với id: " + voucherId));

        return toResponse(voucher);
    }


    // Tao moi 1 voucher
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

        switch (discountType) {
            case "PERCENT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
                voucher.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
                break;

            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
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

    // Kiem tra loai giam gia hop le (PERCENT, AMOUNT, FREESHIP)
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

    // Kiem tra cac truong giam gia (gia tri, gioi han, phan tram)
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

    // Kiem tra khoang thoi gian hop le (ngay bat dau < ngay ket thuc)
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

    // Cap nhat thong tin voucher
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
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : voucher.getQuantity());
                break;
            case "AMOUNT":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : voucher.getQuantity());
                break;
            case "FREESHIP":
                voucher.setDiscountValue(request.getDiscountValue());
                voucher.setMaxDiscountAmount(null);
                voucher.setQuantity(request.getQuantity() != null ? request.getQuantity() : voucher.getQuantity());
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

    // Lay danh sach voucher con han su dung
    @Override
    @Transactional
    public List<VoucherResponse> getAvailableVouchers() {
        return voucherRepository.findAvailableVouchers()
                .stream().map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Kiem tra va ghi nhan viec su dung voucher (tru so luong voucher)
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
        // 2. Kiểm tra số lượng voucher còn không

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

    // Ap dung voucher vao don hang (kiem tra tinh hop le, tinh so tien giam, tru so luong)
    @Override
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

        // 2. Validate voucher
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Mã giảm giá không còn hoạt động.")
                    .build();
        }
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Mã giảm giá chưa có hiệu lực.")
                    .build();
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            return VoucherApplyResult.builder()
                    .applied(false)
                    .message("Mã giảm giá đã hết hạn.")
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

        // 5. Trừ quantity nếu là SPECIFIC (chỉ trừ khi còn quota)
        if ("SPECIFIC".equals(voucher.getTargetType())) {
            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                // Rollback lại UserVoucher nếu quantity hết
                userVoucherRepository.findByVoucherVoucherIdAndUserUserId(voucher.getVoucherId(), userId)
                        .ifPresent(uv -> {
                            uv.setIsUsed(false);
                            uv.setUsedAt(null);
                            userVoucherRepository.save(uv);
                        });
                return VoucherApplyResult.builder()
                        .applied(false)
                        .message("Mã giảm giá đã hết lượt sử dụng.")
                        .build();
            }
            voucher.setQuantity(voucher.getQuantity() - 1);
            // KHÔNG save ngay - để trong transaction của checkout
        } else {
            // ALL voucher: kiểm tra và trừ quantity
            if (voucher.getQuantity() == null || voucher.getQuantity() <= 0) {
                return VoucherApplyResult.builder()
                        .applied(false)
                        .message("Mã giảm giá đã hết lượt sử dụng.")
                        .build();
            }
            voucher.setQuantity(voucher.getQuantity() - 1);
        }

        // 6. Tính discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal freeshipAmount = BigDecimal.ZERO;

        // TODO: FREESHIP voucher - chỉ hiển thị miễn phí ship ở UI
        //       Cần tích hợp lại với GHN khi hoàn thiện cơ chế mới
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

    // Hoan tac viec su dung voucher (tra lai so luong voucher)
    @Override
    public void rollbackVoucherUsage(Integer voucherId, Integer userId) {
        // 1. Tìm UserVoucher record
        Optional<UserVoucher> userVoucherOpt = userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucherId, userId);

        if (userVoucherOpt.isPresent()) {
            UserVoucher userVoucher = userVoucherOpt.get();
            // Chỉ rollback nếu voucher đang ở trạng thái đã used
            if (Boolean.TRUE.equals(userVoucher.getIsUsed())) {
                userVoucher.setIsUsed(false);
                userVoucher.setUsedAt(null);
                userVoucherRepository.save(userVoucher);
            }
        }

        // 2. Tăng lại Voucher.quantity
        Voucher voucher = voucherRepository.findById(voucherId).orElse(null);
        if (voucher != null) {
            voucher.setQuantity(voucher.getQuantity() + 1);
            voucherRepository.save(voucher);
        }
    }

    // Xac nhan viec su dung voucher (luu UserVoucher va save so luong)
    @Override
    public void confirmVoucherUsage(Integer voucherId, Integer userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Update hoặc create UserVoucher record
        Optional<UserVoucher> userVoucherOpt = userVoucherRepository
                .findByVoucherVoucherIdAndUserUserId(voucherId, userId);

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher."));

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

        // 2. Save Voucher.quantity
        // (quantity đã được trừ trong applyVoucher nhưng chưa persist)
        voucherRepository.save(voucher);
    }

    // ========== Voucher Usage Tracking ==========

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

    // Lay lich su su dung voucher (danh sach user da dung)
    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> getVoucherUsageHistory(Integer voucherId) {
        List<UserVoucher> usages = userVoucherRepository.findUsedByVoucherId(voucherId);
        return usages.stream()
                .map(this::toUsageResponse)
                .collect(Collectors.toList());
    }

    // Lay lich su su dung voucher cua 1 user
    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsageResponse> getUserVoucherUsageHistory(Integer userId) {
        List<UserVoucher> usages = userVoucherRepository.findUsedByUserId(userId);
        return usages.stream()
                .map(this::toUsageResponse)
                .collect(Collectors.toList());
    }

    // Dem so lan su dung voucher
    @Override
    @Transactional(readOnly = true)
    public long getVoucherUsedCount(Integer voucherId) {
        return userVoucherRepository.countUsedByVoucherId(voucherId);
    }
}
