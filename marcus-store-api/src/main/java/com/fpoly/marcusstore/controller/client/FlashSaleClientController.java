package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint public dành cho phía client (storefront) để lấy danh sách Flash Sale
 * còn hiệu lực. Logic lọc + sắp xếp theo ưu tiên nghiệp vụ nằm ở Service.
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class FlashSaleClientController {

    private final FlashSaleService flashSaleService;

    /**
     * GET /api/home/flashsales?limit=20
     *
     * Trả về danh sách slot Flash Sale đang diễn ra + sắp diễn ra trong vòng 2h,
     * đã sắp xếp theo ưu tiên:
     *   1. Slot "đang diễn ra" (status=2, now nằm trong [start, end]) lên đầu.
     *      Trong nhóm: slot kết thúc gần nhất (endDate ASC) đứng trước.
     *   2. Slot "sắp diễn ra trong vòng 2h" (status=1, startDate trong (now, now+2h]) ra sau.
     *      Trong nhóm: slot bắt đầu sớm nhất (startDate ASC) đứng trước.
     *   3. Các slot ngoài 2 khung trên bị loại bỏ.
     *
     * Mỗi slot trả về kèm items[] chứa thông tin SKU + giá Flash Sale để FE render card.
     *
     * @param limit Giới hạn số slot trả về (mặc định 20, max 100).
     */
    @GetMapping("/flashsales")
    public List<FlashSaleResponse> getActiveAndUpcomingFlashSales(
            @RequestParam(defaultValue = "20") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return flashSaleService.getActiveAndUpcomingFlashSaleSlots(safeLimit);
    }
}