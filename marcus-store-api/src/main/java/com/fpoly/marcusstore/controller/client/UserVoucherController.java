package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherUsageResponse;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.UserVoucherService;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/vouchers")
@RequiredArgsConstructor
public class UserVoucherController {

    private final UserVoucherService userVoucherService;
    private final VoucherService voucherService;

    // lấy toàn bộ voucher user có thể dùng hiển thij
    @GetMapping("/available")
    public ResponseEntity<List<VoucherResponse>> getAvailableVouchers() {
        return ResponseEntity.ok(userVoucherService.getAvailableVouchersForUser());
    }

    // Lấy Voucher mà user đã sử dụng
    @GetMapping("/my-usage")
    public ResponseEntity<List<VoucherUsageResponse>> getMyVoucherUsageHistory() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<VoucherUsageResponse> usages = voucherService.getUserVoucherUsageHistory(userId);
        return ResponseEntity.ok(usages);
    }
}
