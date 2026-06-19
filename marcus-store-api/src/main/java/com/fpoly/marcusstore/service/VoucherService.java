package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoucherService {
    Page<VoucherResponse> getVouchersPage(String keyword, String discountType, Boolean isActive, Pageable pageable);
    VoucherStatsResponse getVoucherStats(String keyword, String discountType, Boolean isActive);
    VoucherResponse getVoucherById(Integer voucherId);
    void deleteVoucherById(Integer voucherId);
    VoucherResponse addVoucher(AddVoucherRequest request);
    VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request);
}
