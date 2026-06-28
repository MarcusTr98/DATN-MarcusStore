package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.ApplyVoucherRequest;
import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.VoucherApplyResult;
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

    // Lay danh sach voucher ma user hien tai co the su dung
    @GetMapping("/available")
    public ResponseEntity<List<VoucherResponse>> getAvailableVouchers() {
        return ResponseEntity.ok(userVoucherService.getAvailableVouchersForUser());
    }

    // Lay lich su voucher ma user da su dung
    @GetMapping("/my-usage")
    public ResponseEntity<List<VoucherUsageResponse>> getMyVoucherUsageHistory() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<VoucherUsageResponse> usages = voucherService.getUserVoucherUsageHistory(userId);
        return ResponseEntity.ok(usages);
    }

    // Preview voucher de hien thi discount tren FE (khong confirm, khong tru quota)
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<VoucherApplyResult>> previewVoucher(
            @RequestBody ApplyVoucherRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        VoucherApplyResult result = voucherService.applyVoucher(request, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
