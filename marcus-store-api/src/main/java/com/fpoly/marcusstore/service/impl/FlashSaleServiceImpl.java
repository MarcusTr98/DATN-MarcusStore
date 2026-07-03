package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class FlashSaleServiceImpl implements FlashSaleService {
     private final FlashSaleSlotRepository flashSaleSlotRepository;
     private final FlashSaleItemRepository flashSaleItemRepository;

     /**
      * Map 1 slot sang FlashSaleResponse, lấy tổng số lượng từ map batch để tránh N+1.
      */
     private FlashSaleResponse toResponse(FlashSaleSlot slot, Map<Integer, Integer> qtyMap) {
          return FlashSaleResponse.builder()
                  .slotId(slot.getSlotId())
                  .name(slot.getName())
                  .startDate(slot.getStartDate())
                  .endDate(slot.getEndDate())
                  .status(slot.getStatus())
                  .quantityFlashSaleSlot(qtyMap.getOrDefault(slot.getSlotId(), 0))
                  .createdAt(slot.getCreatedAt())
                  .updatedAt(slot.getUpdatedAt())
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
         Page<FlashSaleSlot> page = flashSaleSlotRepository
                 .searchFlashSaleSlots(normalizedKeyword, status, LocalDateTime.now(), pageable);

         // Lấy tổng quantity theo batch trong 1 query duy nhất
         List<Integer> slotIds = page.getContent().stream()
                 .map(FlashSaleSlot::getSlotId)
                 .toList();

         Map<Integer, Integer> qtyMap = slotIds.isEmpty()
                 ? Map.of()
                 : flashSaleItemRepository.sumFlashSaleQuantityBySlotIds(slotIds).stream()
                         .collect(Collectors.toMap(
                                 row -> (Integer) row[0],
                                 row -> ((Number) row[1]).intValue()));

         return page.map(slot -> toResponse(slot, qtyMap));
     }
    // lấy số lượng slot, số lượng slot đang chạy, số lượng slot sắp chạy, tổng số sản phẩm của toàn bộ slot
    @Override
    @Transactional(readOnly = true)
    public FlashSaleStatsResponse getFlashSaleStats(String keyword, Short status) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Short normalizedStatus = status;
        LocalDateTime now = LocalDateTime.now();
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

          // Vẫn dùng batch API, truyền 1 id
          Map<Integer, Integer> qtyMap = flashSaleItemRepository.sumFlashSaleQuantityBySlotIds(List.of(slotId))
                  .stream()
                  .collect(Collectors.toMap(
                          row -> (Integer) row[0],
                          row -> ((Number) row[1]).intValue()));

          return toResponse(slot, qtyMap);
     }

}
