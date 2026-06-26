package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddVoucherRequest;
import com.fpoly.marcusstore.dto.request.ApplyVoucherRequest;
import com.fpoly.marcusstore.dto.response.VoucherApplyResult;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.dto.response.VoucherStatsResponse;
import com.fpoly.marcusstore.dto.response.VoucherUsageResponse;
import com.fpoly.marcusstore.entity.shopping.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VoucherService {
    // lấy danh sách voucher phân trang theo lại keyword, loại giảm giá, đang sử dụng
    Page<VoucherResponse> getVouchersPage(String keyword, String discountType, Boolean isActive, Pageable pageable);
    // lấy danh sách thoogns kê theo loại keyword, loại giảm giá, đang sử dụng
    VoucherStatsResponse getVoucherStats(String keyword, String discountType, Boolean isActive);
    // lấy chi tiết 1 voucher theo Id
    VoucherResponse getVoucherById(Integer voucherId);
    // taoj mới voucher
    VoucherResponse addVoucher(AddVoucherRequest request);
    // update voucher
    VoucherResponse updateVoucher(Integer voucherId, AddVoucherRequest request);
    // lấy danh sách voucher dùng được
    List<VoucherResponse> getAvailableVouchers();
    // kiểm
    boolean checkAndRecordVoucherUsage(Integer voucherId, Integer userId);
    // Ap dung voucher vao don hang, tra ve so tien giam
    VoucherApplyResult applyVoucher(ApplyVoucherRequest request, Integer userId);
    // Hoan tac viec su dung voucher (tra lai so luong)
    void rollbackVoucherUsage(Integer voucherId, Integer userId);
    // Xac nhan viec su dung voucher (luu vao database)
    void confirmVoucherUsage(Integer voucherId, Integer userId);

    // Lay lich su su dung voucher (admin xem)
    List<VoucherUsageResponse> getVoucherUsageHistory(Integer voucherId);
    // Lay lich su su dung voucher (user xem)
    List<VoucherUsageResponse> getUserVoucherUsageHistory(Integer userId);
    // Dem so lan su dung voucher
    long getVoucherUsedCount(Integer voucherId);
}
