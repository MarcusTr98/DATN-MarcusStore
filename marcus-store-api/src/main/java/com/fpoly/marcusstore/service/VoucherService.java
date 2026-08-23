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
    // ngừng hoạt động (soft-delete) voucher theo ID
    void deleteVoucher(Integer voucherId);
    // áp dụng voucher vào đơn và return về tiền giảm
    VoucherApplyResult applyVoucher(ApplyVoucherRequest request, Integer userId);
    // Xac nhan viec su dung voucher (luu vao database)
    void confirmVoucherUsage(Integer voucherId, Integer userId);

    // get lịch sử sử dụng voucher của user (admin xem)
    List<VoucherUsageResponse> getVoucherUsageHistory(Integer voucherId);
    // get lịch sử sử dụng voucher (user xem)
    List<VoucherUsageResponse> getUserVoucherUsageHistory(Integer userId);
    // đếm số lần voucher được dừng
    long getVoucherUsedCount(Integer voucherId);
    // Marcus thêm: deactivate tất cả voucher hết hạn hoặc hết quantity
    int deactivateExpiredVouchers();
}
