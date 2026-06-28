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

   // get các user được gán voucher
    @GetMapping("/voucher/{voucherId}/users")
    public ResponseEntity<List<VoucherResponse>> getUsersByVoucher(@PathVariable Integer voucherId) {
        List<VoucherResponse> users = userVoucherService.getUserVouchersByVoucherId(voucherId);
        return ResponseEntity.ok(users);
    }

    // thêm voucher cho từng user
    @PostMapping("/assign")
    public ResponseEntity<Void> assignVoucherToUsers(
            @RequestParam Integer voucherId,
            @RequestBody List<Integer> userIds) {
        userVoucherService.assignVoucherToUsers(voucherId, userIds);
        return ResponseEntity.ok().build();
    }

    // update thêm user mới xóa bỏ user cũ
    @PutMapping("/voucher/{voucherId}/users")
    public ResponseEntity<Void> reassignVoucherUsers(
            @PathVariable Integer voucherId,
            @RequestBody List<Integer> userIds) {
        userVoucherService.reassignVoucherUsers(voucherId, userIds);
        return ResponseEntity.ok().build();
    }
}
