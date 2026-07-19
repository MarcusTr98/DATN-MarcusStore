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


    // Đổi trạng thái nhanh (admin): dùng cho hủy chiến dịch đã lên lịch (status 1 -> 4)
    // hoặc hủy chiến dịch đang diễn ra (status 2 -> 4).
    // hoặc các thao tác đổi status nhanh khác. Trả về slot sau khi cập nhật.
    FlashSaleResponse updateFlashSaleStatus(Integer slotId, Short status);

    // Khôi phục flash sale đã bị hủy (CANCELLED -> ACTIVE).
    // Điều kiện:
    //   1. Thời gian: now < (endDate - 1 tiếng)
    //   2. Không trùng khung giờ với slot ACTIVE/SCHEDULED khác trong khoảng [now, endDate]
    // Trả về slot sau khi khôi phục (status = ACTIVE).
    FlashSaleResponse restoreFlashSale(Integer slotId);

    // Scheduled: auto-update status theo thời gian + khoá tổng SP khi hết hạn
    void autoUpdateFlashSaleStatuses();

    // Kiểm tra 1 khoảng thời gian có đang đụng với slot khác không
    // Trả về danh sách slot overlap để FE hiện cảnh báo trước khi submit
    java.util.List<FlashSaleResponse> checkOverlappingSlots(
            java.time.LocalDateTime startDate,
            java.time.LocalDateTime endDate,
            Integer excludeSlotId);

    // Client: lấy danh sách slot đang diễn ra + sắp diễn ra trong vòng 2h
    // Trả về FlashSaleResponse kèm items[] để FE render card sản phẩm.
    // Sắp xếp theo yêu cầu nghiệp vụ:
    //   1. Ưu tiên slot "đang diễn ra" (status=2, now nằm giữa startDate và endDate),
    //      trong nhóm này slot kết thúc gần nhất đứng trước (endDate ASC).
    //   2. Tiếp theo slot "sắp diễn ra trong vòng 2h" (status=1, startDate trong (now, now+2h]),
    //      trong nhóm này slot bắt đầu sớm nhất đứng trước (startDate ASC).
    //   3. Các slot ngoài 2 khung trên bị loại bỏ.
    // Tham số 'limit' giới hạn tổng số slot trả về (mặc định 20).
    java.util.List<FlashSaleResponse> getActiveAndUpcomingFlashSaleSlots(int limit);
}
