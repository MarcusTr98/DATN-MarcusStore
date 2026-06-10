package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.GetExchange;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class VoucherController {
    private final VoucherService voucherService;
    @GetMapping("/vouchers")
    public List<VoucherResponse> getAllVoucher(){
        return voucherService.getAllVouChers();
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
    public ResponseEntity<VoucherResponse> addVoucher(@RequestBody AddVoucherRequest request){
        VoucherResponse response =  voucherService.addVoucher(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
