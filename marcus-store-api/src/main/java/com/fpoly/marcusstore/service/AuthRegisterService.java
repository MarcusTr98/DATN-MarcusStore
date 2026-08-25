package com.fpoly.marcusstore.service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.marcusstore.dto.request.RegisterRequest;
import com.fpoly.marcusstore.dto.request.VerifyOtpRequest;
import com.fpoly.marcusstore.entity.auth.EmailOTP;
import com.fpoly.marcusstore.entity.auth.PendingRegistration;
import com.fpoly.marcusstore.entity.auth.Role;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import com.fpoly.marcusstore.repository.auth.EmailOTPRepository;
import com.fpoly.marcusstore.repository.auth.PendingRegistrationRepository;
import com.fpoly.marcusstore.repository.auth.RoleRepository;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.repository.promotion.VoucherRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class AuthRegisterService {

    private static final Logger log = LoggerFactory.getLogger(AuthRegisterService.class);

    // Voucher chào mừng cho user đăng ký qua form newsletter ở footer.
    // Mỗi user được TẠO MỚI 1 voucher riêng, mã dạng WELCOME100K-U{userId}, chỉ user đó dùng được.
    private static final String PERSONAL_VOUCHER_PREFIX = "WELCOME100K-U";
    private static final BigDecimal PERSONAL_VOUCHER_AMOUNT = new BigDecimal("100000");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailOTPRepository otpRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EntityManager entityManager; 
    private final CartRepository cartRepository;
    private final VoucherRepository voucherRepository;             // thêm
    private final UserVoucherService userVoucherService;            // thêm

       /** Dùng cho form "Nhận ưu đãi độc quyền" ở footer: kiểm tra email đã có tài khoản chưa. */
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void requestRegister(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

   
        entityManager.createNativeQuery(
            "DELETE FROM PendingRegistrations WHERE email = :email"
        ).setParameter("email", request.getEmail()).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM PendingRegistrations WHERE username = :username"
        ).setParameter("username", request.getUsername()).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM EmailOtps WHERE email = :email"
        ).setParameter("email", request.getEmail()).executeUpdate();

        PendingRegistration pending = new PendingRegistration();
        pending.setUsername(request.getUsername());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setEmail(request.getEmail());
        pending.setPhoneNumber(request.getPhoneNumber());
        pending.setFullName(request.getFullName());
        pending.setCreatedAt(LocalDateTime.now());
        pending.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        pending.setNewsletterSignup(Boolean.TRUE.equals(request.getNewsletterSignup())); // thêm
        pendingRepository.save(pending);

        String otp = String.format("%06d",
                ThreadLocalRandom.current().nextInt(100000, 1000000));

        EmailOTP emailOtp = new EmailOTP();
        emailOtp.setEmail(request.getEmail());
        emailOtp.setOtpCode(otp);
        emailOtp.setCreatedAt(LocalDateTime.now());
        emailOtp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(emailOtp);

        emailService.sendOtp(request.getEmail(), otp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW) 
    public String verifyRegister(VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtp());

        PendingRegistration pending = pendingRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đăng ký"));

        Role role = roleRepository
                .findByRoleName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        User user = new User();
        user.setRole(role);
        user.setUsername(pending.getUsername());
        user.setPasswordHash(pending.getPasswordHash());
        user.setEmail(pending.getEmail());
        user.setPhoneNumber(pending.getPhoneNumber());
        user.setFullName(pending.getFullName());
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setCreatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        if (Boolean.TRUE.equals(pending.getNewsletterSignup())) {
            grantWelcomeVoucher(user.getUserId());
        }

        pendingRepository.deleteByEmail(request.getEmail());

        return "Đăng ký thành công";
    }

    /**
     * Tạo 1 voucher chào mừng RIÊNG cho user vừa đăng ký từ form newsletter
     * (mã dạng WELCOME100K-U{userId}, quantity = 1, chỉ user này dùng được).
     * Cố tình không throw nếu thất bại — voucher là quà tặng thêm,
     * không được phép làm hỏng cả luồng đăng ký tài khoản.
     */
    private void grantWelcomeVoucher(Integer userId) {
        try {
            String code = PERSONAL_VOUCHER_PREFIX + userId;

            // Phòng trường hợp verify bị gọi lại / trùng — không tạo voucher thứ 2 cho cùng 1 user
            if (Boolean.TRUE.equals(voucherRepository.existsByVoucherCode(code))) {
                log.warn("User {} đã có voucher chào mừng riêng ({}) — bỏ qua.", userId, code);
                return;
            }

            Voucher voucher = new Voucher();
            voucher.setVoucherCode(code);
            voucher.setDiscountValue(PERSONAL_VOUCHER_AMOUNT);
            voucher.setDiscountType("AMOUNT");
            voucher.setMaxDiscountAmount(null);
            voucher.setMinOrderValue(BigDecimal.ZERO);
            voucher.setStartDate(LocalDateTime.now());
            voucher.setEndDate(LocalDateTime.now().plusYears(1));
            voucher.setQuantity(1);
            voucher.setStatus(Voucher.STATUS_ACTIVE);
            voucher.setTargetType("SPECIFIC");

            voucher = voucherRepository.save(voucher);

            userVoucherService.assignVoucherToUsers(voucher.getVoucherId(), List.of(userId));
        } catch (Exception ex) {
            log.error("Tạo voucher chào mừng riêng thất bại cho user {}: {}", userId, ex.getMessage(), ex);
        }
    }
}