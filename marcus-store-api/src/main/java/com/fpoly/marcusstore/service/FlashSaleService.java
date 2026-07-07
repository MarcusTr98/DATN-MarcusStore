package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.FlashSaleSlotRequest;
import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FlashSaleService {

    // Phân trang
    Page<FlashSaleResponse> getFlashSaleSlotsPage(String keyword, Short status, Pageable pageable);
    // Stats
    FlashSaleStatsResponse getFlashSaleStats(String keyword, Short status);
    // Chi tiết
    FlashSaleResponse getFlashSaleSlotById(Integer slotId);
    // Tạo mới
   FlashSaleResponse createFlashSale(FlashSaleSlotRequest request);

    // Cập nhật
    FlashSaleResponse updateFlashSale(Integer slotId, FlashSaleSlotRequest request);

    // Scheduled: auto-update status theo thời gian + khoá tổng SP khi hết hạn
    void autoUpdateFlashSaleStatuses();

    // Kiểm tra 1 khoảng thời gian có đang đụng với slot khác không
    // Trả về danh sách slot overlap để FE hiện cảnh báo trước khi submit
    java.util.List<FlashSaleResponse> checkOverlappingSlots(
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Integer excludeSlotId);
}
