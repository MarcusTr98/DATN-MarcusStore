package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class FlashSaleServiceImpl implements FlashSaleService {
     private final FlashSaleSlotRepository flashSaleSlotRepository;
     private FlashSaleResponse toResponses(FlashSaleSlot flashSaleSlot){
          Integer quantity = flashSaleSlotRepository.countTotalQuantityBySlotId(flashSaleSlot.getSlotId());
          return FlashSaleResponse.builder()
                  .slotId(flashSaleSlot.getSlotId())
                  .name(flashSaleSlot.getName())
                  .startDate(flashSaleSlot.getStartDate())
                  .endDate(flashSaleSlot.getEndDate())
                  .status(flashSaleSlot.getStatus())
                  .quantityFlashSaleSlot(quantity)
                  .createdAt(flashSaleSlot.getCreatedAt())
                  .updatedAt(flashSaleSlot.getUpdatedAt())
                  .build();
     }
     // chuẩn hóa keyword người dùng gửi
     private String normalizeKeyword(String keyword) {
          return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
     }
     // phân trang
     @Override
     @Transactional(readOnly = true)
     public Page<FlashSaleResponse> getFlashSaleSlotsPage(String keyword, Short status, Pageable pageable) {
          String normalizedKeyword = normalizeKeyword(keyword);
          Short normalizedStatus =status;
          return flashSaleSlotRepository
                  .searchFlashSaleSlots(normalizedKeyword, normalizedStatus, pageable)
                  .map(this::toResponses);
     }
    // lấy số lượng slot, số lượng slot đang chạy, số lượng slot sắp chạy, tổng số sản phẩm của toàn bộ slot
    @Override
    @Transactional(readOnly = true)
    public FlashSaleStatsResponse getFlashSaleStats(String keyword, Short status) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Short normalizedStatus = status;
        LocalDateTime now = LocalDateTime.now();
        // #region DEBUG_LOG
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                new java.net.URL("http://127.0.0.1:7828/ingest/6683b584-65cf-4bee-bf05-7d2708749dc3")
                .openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String payload = String.format(
                "{\"sessionId\":\"41c373\",\"location\":\"FlashSaleServiceImpl.java:58\",\"message\":\"getFlashSaleStats input params\",\"data\":{\"keyword\":\"%s\",\"status\":%s,\"now\":\"%s\"},\"timestamp\":%d}",
                normalizedKeyword == null ? "null" : normalizedKeyword,
                normalizedStatus == null ? "null" : normalizedStatus.toString(),
                now.toString(),
                System.currentTimeMillis()
            );
            try (var os = conn.getOutputStream()) {
                os.write(payload.getBytes());
            }
            conn.disconnect();
        } catch (Exception ex) { /* silent */ }
        // #endregion
        // #region DEBUG_LOG
        try {
            java.net.HttpURLConnection conn2 = (java.net.HttpURLConnection)
                new java.net.URL("http://127.0.0.1:7828/ingest/6683b584-65cf-4bee-bf05-7d2708749dc3")
                .openConnection();
            conn2.setRequestMethod("POST");
            conn2.setRequestProperty("Content-Type", "application/json");
            conn2.setDoOutput(true);
            long total = flashSaleSlotRepository.countFlashSaleSlots(normalizedKeyword, normalizedStatus);
            long active = flashSaleSlotRepository.countActiveSlots(normalizedKeyword, normalizedStatus, now);
            long upcoming = flashSaleSlotRepository.countUpcomingSlots(normalizedKeyword, normalizedStatus, now);
            long products = flashSaleSlotRepository.sumFlashSaleQuantityInActiveSlots(now);
            String payload2 = String.format(
                "{\"sessionId\":\"41c373\",\"location\":\"FlashSaleServiceImpl.java:75\",\"message\":\"getFlashSaleStats query results\",\"data\":{\"totalSlots\":%d,\"activeSlots\":%d,\"upcomingSlots\":%d,\"totalActiveProducts\":%d},\"timestamp\":%d}",
                total, active, upcoming, products, System.currentTimeMillis()
            );
            try (var os = conn2.getOutputStream()) {
                os.write(payload2.getBytes());
            }
            conn2.disconnect();
        } catch (Exception ex2) { /* silent */ }
        // #endregion
        return FlashSaleStatsResponse.builder()
                .totalSlots(flashSaleSlotRepository
                        .countFlashSaleSlots(normalizedKeyword, normalizedStatus))
                .activeSlots(flashSaleSlotRepository
                        .countActiveSlots(normalizedKeyword, normalizedStatus, now))
                .upcomingSlots(flashSaleSlotRepository
                        .countUpcomingSlots(normalizedKeyword, normalizedStatus, now))
                .totalActiveProducts(flashSaleSlotRepository
                        .sumFlashSaleQuantityInActiveSlots(now))
                .build();
    }
     @Override
     @Transactional(readOnly = true)
     public FlashSaleResponse getFlashSaleSlotById(Integer slotId) {
          FlashSaleSlot slot = flashSaleSlotRepository.findById(slotId)
                  .orElseThrow(() -> new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "Không tìm thấy flash sale với id: " + slotId));
          return toResponses(slot);
     }

}
