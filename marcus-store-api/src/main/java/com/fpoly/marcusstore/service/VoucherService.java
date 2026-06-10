package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.entity.shopping.Voucher;

import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAllVouChers();
    VoucherResponse getVoucherById(Integer voucherId);
    void deleteVoucherById(Integer voucherId);
    VoucherResponse addVoucher(AddVoucherRequest request);
    VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request);
}
