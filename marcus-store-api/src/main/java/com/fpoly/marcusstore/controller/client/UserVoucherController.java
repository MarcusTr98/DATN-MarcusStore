package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client/vouchers")
@RequiredArgsConstructor
public class UserVoucherController {

    private final UserVoucherService userVoucherService;

    @GetMapping("/available")
    public ResponseEntity<List<VoucherResponse>> getAvailableVouchers() {
        return ResponseEntity.ok(userVoucherService.getAvailableVouchersForUser());
    }
}
