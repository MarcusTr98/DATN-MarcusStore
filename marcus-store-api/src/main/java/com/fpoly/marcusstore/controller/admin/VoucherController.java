package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
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
    ){
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "voucherId")
        );

        return voucherService.getVouchersPage(keyword, discountType, isActive, pageable);
    }

    @GetMapping("/vouchers/stats")
    public VoucherStatsResponse getVoucherStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Boolean isActive
    ){
        return voucherService.getVoucherStats(keyword, discountType, isActive);
    }

    @GetMapping("/voucher/{voucherId}")
    public VoucherResponse getVoucherById(@PathVariable("voucherId") Integer voucherId){
        return voucherService.getVoucherById(voucherId);
    }
    @DeleteMapping("/voucher/{voucherId}")
    public ResponseEntity<Void> removeVoucher(@PathVariable("voucherId") Integer voucherId){
        voucherService.deleteVoucherById(voucherId);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/voucher")
    public ResponseEntity<VoucherResponse> addVoucher(@Valid @RequestBody AddVoucherRequest request){
        VoucherResponse response =  voucherService.addVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PutMapping("/voucher/{voucherId}")
    public ResponseEntity<VoucherResponse> updateVoucher( @PathVariable("voucherId") Integer voucherId,@Valid @RequestBody AddVoucherRequest request){
        VoucherResponse voucher = voucherService.updateVoucher(voucherId, request);
        return ResponseEntity.status(HttpStatus.OK).body(voucher);
    }
}
