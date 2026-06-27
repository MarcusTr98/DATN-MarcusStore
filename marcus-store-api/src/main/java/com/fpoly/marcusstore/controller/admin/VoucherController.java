package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.dto.response.VoucherUsageResponse;
import com.fpoly.marcusstore.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('MARKETING_VIEW')")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @GetMapping("/vouchers")
    public Page<VoucherResponse> getAllVoucher(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Boolean isActive
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "voucherId")
        );

        return voucherService.getVouchersPage(keyword, discountType, isActive, pageable);
    }

    // Lấy thống kê voucher
    @GetMapping("/vouchers/stats")
    public VoucherStatsResponse getVoucherStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Boolean isActive
    ) {
        return voucherService.getVoucherStats(keyword, discountType, isActive);
    }

    // Lấy chi tiết voucher
    @GetMapping("/voucher/{voucherId}")
    public VoucherResponse getVoucherById(@PathVariable("voucherId") Integer voucherId) {
        return voucherService.getVoucherById(voucherId);
    }

    // Xóa voucher
    // @DeleteMapping("/voucher/{voucherId}")
    // @PreAuthorize("hasAuthority('MARKETING_DELETE')")
    // public ResponseEntity<Void> removeVoucher(@PathVariable("voucherId") Integer voucherId){
    //     voucherService.deleteVoucherById(voucherId);
    //     return ResponseEntity.noContent().build();
    // }

    // Thêm voucher
    @PostMapping("/voucher")
    @PreAuthorize("hasAuthority('MARKETING_CREATE')")
    public ResponseEntity<VoucherResponse> addVoucher(@Valid @RequestBody AddVoucherRequest request) {
        VoucherResponse response = voucherService.addVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Cập nhật voucher
    @PutMapping("/voucher/{voucherId}")
    @PreAuthorize("hasAuthority('MARKETING_UPDATE')")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable("voucherId") Integer voucherId,
            @Valid @RequestBody AddVoucherRequest request) {

        VoucherResponse response = voucherService.updateVoucher(voucherId, request);
        return ResponseEntity.ok(response);
    }

    // Lấy danh sách user đã dùng voucher
    @GetMapping("/voucher/{voucherId}/usage")
    public ResponseEntity<List<VoucherUsageResponse>> getVoucherUsageHistory(
            @PathVariable("voucherId") Integer voucherId) {

        List<VoucherUsageResponse> usages = voucherService.getVoucherUsageHistory(voucherId);
        return ResponseEntity.ok(usages);
    }

    // Đếm số lần voucher được sử dụng
    @GetMapping("/voucher/{voucherId}/usage-count")
    public ResponseEntity<Long> getVoucherUsedCount(
            @PathVariable("voucherId") Integer voucherId) {

        long count = voucherService.getVoucherUsedCount(voucherId);
        return ResponseEntity.ok(count);
    }
}