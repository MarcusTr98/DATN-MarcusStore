package com.fpoly.marcusstore.service;

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

}
