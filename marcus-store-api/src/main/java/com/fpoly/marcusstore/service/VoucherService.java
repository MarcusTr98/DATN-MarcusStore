package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VoucherService {
    Page<VoucherResponse> getVouchersPage(String keyword, String discountType, Boolean isActive, Pageable pageable);
    VoucherStatsResponse getVoucherStats(String keyword, String discountType, Boolean isActive);
    VoucherResponse getVoucherById(Integer voucherId);
    void deleteVoucherById(Integer voucherId);
    VoucherResponse addVoucher(AddVoucherRequest request);
    VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request);
    List<VoucherResponse> getAvailableVouchers();

    /**
     * Kiểm tra và ghi nhận việc sử dụng voucher của user
     * @param voucherId ID của voucher
     * @param userId ID của user
     * @return true nếu user được phép dùng voucher và đã ghi nhận thành công
     * @throws RuntimeException nếu voucher không hợp lệ hoặc user đã dùng rồi
     */
    boolean checkAndRecordVoucherUsage(Integer voucherId, Integer userId);
}
