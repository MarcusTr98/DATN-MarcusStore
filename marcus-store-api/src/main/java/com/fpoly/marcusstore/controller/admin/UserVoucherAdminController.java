package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user-vouchers")
@RequiredArgsConstructor
public class UserVoucherAdminController {

    private final UserVoucherService userVoucherService;

    /**
     * Lấy danh sách user được gán voucher
     */
    @GetMapping("/voucher/{voucherId}/users")
    public ResponseEntity<List<VoucherResponse>> getUsersByVoucher(@PathVariable Integer voucherId) {
        List<VoucherResponse> users = userVoucherService.getUserVouchersByVoucherId(voucherId);
        return ResponseEntity.ok(users);
    }

    /**
     * Gán voucher cho nhiều user (thêm user mới, giữ nguyên user đã có)
     */
    @PostMapping("/assign")
    public ResponseEntity<Void> assignVoucherToUsers(
            @RequestParam Integer voucherId,
            @RequestBody List<Integer> userIds) {
        userVoucherService.assignVoucherToUsers(voucherId, userIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Cập nhật danh sách user cho voucher
     * - Xóa những user không còn trong danh sách
     * - Thêm những user mới chưa có
     * - Giữ nguyên những user đã có
     */
    @PutMapping("/voucher/{voucherId}/users")
    public ResponseEntity<Void> reassignVoucherUsers(
            @PathVariable Integer voucherId,
            @RequestBody List<Integer> userIds) {
        userVoucherService.reassignVoucherUsers(voucherId, userIds);
        return ResponseEntity.ok().build();
    }
}
