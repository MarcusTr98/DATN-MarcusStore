package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.FlashSaleItemRequest;
import com.fpoly.marcusstore.dto.request.FlashSaleSlotRequest;
import com.fpoly.marcusstore.dto.response.FlashSaleItemResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItemId;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleSlotRepository;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashSaleServiceImpl implements FlashSaleService {
     private final FlashSaleSlotRepository flashSaleSlotRepository;
     private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductSkuRepository productSkuRepository;

     //  Map 1 slot sang FlashSaleResponse, lấy tổng số lượng từ map batch để tránh N+1.
     private FlashSaleResponse toResponse(FlashSaleSlot slot,
                                          Map<Integer, Integer> qtyMap,
                                          Map<Integer, Integer> usedMap) {
          return FlashSaleResponse.builder()
                  .slotId(slot.getSlotId())
                  .name(slot.getName())
                  .startDate(slot.getStartDate())
                  .endDate(slot.getEndDate())
                  .status(slot.getStatus())
                  .quantityFlashSaleSlot(qtyMap.getOrDefault(slot.getSlotId(), 0))
                  .usedQuantity(usedMap.getOrDefault(slot.getSlotId(), 0))
                  .createdAt(slot.getCreatedAt())
                  .updatedAt(slot.getUpdatedAt())
                  .bannerImageUrl(slot.getBannerImageUrl())
                  .build();
     }
    // Map 1 slot sang FlashSaleResponse (kèm items, dùng cho chi tiết)
    private FlashSaleResponse toResponseWithItems(FlashSaleSlot slot, List<FlashSaleItem> items) {
        List<FlashSaleItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();
        return FlashSaleResponse.builder()
                .slotId(slot.getSlotId())
                .name(slot.getName())
                .startDate(slot.getStartDate())
                .endDate(slot.getEndDate())
                .status(slot.getStatus())
                .quantityFlashSaleSlot(items.stream()
                        .mapToInt(FlashSaleItem::getFlashSaleQuantity)
                        .sum())
                .usedQuantity(items.stream()
                        .mapToInt(i -> i.getSoldQuantity() == null ? 0 : i.getSoldQuantity())
                        .sum())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .bannerImageUrl(slot.getBannerImageUrl())
                .items(itemResponses)
                .build();
    }

    // Map 1 FlashSaleItem sang FlashSaleItemResponse
    private FlashSaleItemResponse toItemResponse(FlashSaleItem item) {
        ProductSku sku = item.getSku();
        Integer remaining = item.getFlashSaleQuantity() - item.getSoldQuantity();
        return FlashSaleItemResponse.builder()
                .skuId(item.getId().getSkuId())
                .productId(sku != null && sku.getProduct() != null ? sku.getProduct().getProductId() : null)
                .productName(sku != null && sku.getProduct() != null ? sku.getProduct().getProductName() : null)
                .skuCode(sku != null ? sku.getSkuCode() : null)
                .skuImageUrl(sku != null ? sku.getSkuImageUrl() : null)
                .originalPrice(item.getOriginalPrice())
                .flashSalePrice(item.getFlashSalePrice())
                .flashSaleQuantity(item.getFlashSaleQuantity())
                .soldQuantity(item.getSoldQuantity())
                .remainingQuantity(remaining)
                .createdAt(item.getCreatedAt())
                .build();
    }
     // chuẩn hóa keyword người dùng gửi
     private String normalizeKeyword(String keyword) {
          return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
     }

     /**
      * Validate toàn bộ request trước khi tạo/cập nhật slot.
      * - Thời gian hợp lệ (startDate không ở quá khứ, endDate > startDate)
      * - Items không null/rỗng, không trùng SKU trong cùng request
      * - Mọi SKU đều tồn tại trong DB
      * - Giá hợp lệ (originalPrice > 0, 0 < flashSalePrice < originalPrice)
      * - Số lượng hợp lệ (flashSaleQuantity >= 1, không vượt tồn kho SKU)
      */
     private void validateRequest(FlashSaleSlotRequest request,
                                  Map<Integer, ProductSku> skuMap) {
          // 1. Thời gian
          if (request.getStartDate() == null || request.getEndDate() == null) {
               throw new ResponseStatusException(
                       HttpStatus.BAD_REQUEST,
                       "Ngày bắt đầu và ngày kết thúc không được để trống");
          }
          LocalDateTime now = LocalDateTime.now();
          if (request.getStartDate().isBefore(now)) {
               throw new ResponseStatusException(
                       HttpStatus.BAD_REQUEST,
                       "Ngày bắt đầu không được ở trong quá khứ");
          }
          if (!request.getEndDate().isAfter(request.getStartDate())) {
               throw new ResponseStatusException(
                       HttpStatus.BAD_REQUEST,
                       "Ngày kết thúc phải sau ngày bắt đầu");
          }

          // 2. Items phải có ít nhất 1 phần tử
          List<FlashSaleItemRequest> itemRequests = request.getItems();
          if (itemRequests == null || itemRequests.isEmpty()) {
               throw new ResponseStatusException(
                       HttpStatus.BAD_REQUEST,
                       "Phải có ít nhất 1 sản phẩm trong Flash Sale");
          }

          // 3. Validate từng item: không trùng SKU + SKU tồn tại + giá/số lượng hợp lệ
          Set<Integer> seenSkuIds = new HashSet<>();
          for (FlashSaleItemRequest ir : itemRequests) {
               // 3a. SKU không được null
               if (ir.getSkuId() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "SKU id không được để trống");
               }
               // 3b. Không trùng SKU trong cùng request
               if (!seenSkuIds.add(ir.getSkuId())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "SKU id bị trùng trong request: " + ir.getSkuId());
               }
               // 3c. SKU phải tồn tại trong DB
               ProductSku sku = skuMap.get(ir.getSkuId());
               if (sku == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Không tìm thấy SKU với id: " + ir.getSkuId());
               }
               // 3d. Validate giá gốc
               if (ir.getOriginalPrice() == null
                       || ir.getOriginalPrice().signum() <= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Giá gốc của SKU " + ir.getSkuId() + " phải > 0");
               }
               // 3e. Validate giá Flash Sale
               if (ir.getFlashSalePrice() == null
                       || ir.getFlashSalePrice().signum() <= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Giá Flash Sale của SKU " + ir.getSkuId() + " phải > 0");
               }
               // 3f. Giá Flash Sale phải nhỏ hơn giá gốc
               if (ir.getFlashSalePrice().compareTo(ir.getOriginalPrice()) >= 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            String.format(
                                    "Giá Flash Sale (%s) phải nhỏ hơn giá gốc (%s) của SKU %d",
                                    ir.getFlashSalePrice().toPlainString(),
                                    ir.getOriginalPrice().toPlainString(),
                                    ir.getSkuId()));
               }
               // 3g. Validate số lượng
               if (ir.getFlashSaleQuantity() == null || ir.getFlashSaleQuantity() < 1) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Số lượng Flash Sale của SKU " + ir.getSkuId() + " phải >= 1");
               }
               // 3h. Không vượt tồn kho SKU (nếu có thông tin tồn kho)
               Integer stock = sku.getStockQuantity();
               if (stock != null && ir.getFlashSaleQuantity() > stock) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            String.format(
                                    "Số lượng Flash Sale của SKU %d vượt tồn kho (còn %d)",
                                    ir.getSkuId(), stock));
               }
          }
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

         // Tổng sản phẩm đã sử dụng (soldQuantity) theo batch
         Map<Integer, Integer> usedMap = slotIds.isEmpty()
                 ? Map.of()
                 : flashSaleItemRepository.sumSoldQuantityBySlotIds(slotIds).stream()
                         .collect(Collectors.toMap(
                                 row -> (Integer) row[0],
                                 row -> ((Number) row[1]).intValue()));

         return page.map(slot -> toResponse(slot, qtyMap, usedMap));
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

          // Tổng soldQuantity cho cột 'Đã sử dụng'
          Map<Integer, Integer> usedMap = flashSaleItemRepository.sumSoldQuantityBySlotIds(List.of(slotId))
                  .stream()
                  .collect(Collectors.toMap(
                          row -> (Integer) row[0],
                          row -> ((Number) row[1]).intValue()));

          return toResponse(slot, qtyMap, usedMap);
     }
    @Override
    @Transactional
    public FlashSaleResponse createFlashSale(FlashSaleSlotRequest request) {
        // 1. Lấy danh sách SKU từ request + build skuMap (để validate tồn tại)
        List<FlashSaleItemRequest> itemRequests = request.getItems();
        List<Integer> skuIds = itemRequests.stream()
                .map(FlashSaleItemRequest::getSkuId)
                .toList();
        List<ProductSku> skus = productSkuRepository.findBySkuIdIn(skuIds);
        Map<Integer, ProductSku> skuMap = skus.stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, s -> s));

        // 2. Validate toàn bộ request (thời gian, SKU trùng, SKU tồn tại,
        //    giá > 0, flashSalePrice < originalPrice, số lượng hợp lệ, không vượt tồn kho)
        validateRequest(request, skuMap);

        // 3. Chặn tạo 2 slot flash sale chạy cùng khung giờ

        List<FlashSaleSlot> overlapping = flashSaleSlotRepository
                .findOverlappingSlots(request.getStartDate(), request.getEndDate(), null);
        if (!overlapping.isEmpty()) {
            String detail = overlapping.stream()
                    .map(s -> String.format("#%d '%s' (%s → %s)",
                            s.getSlotId(),
                            s.getName(),
                            s.getStartDate(),
                            s.getEndDate()))
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khung giờ đã bị trùng với các flash sale khác: " + detail);
        }

        // 4. Tạo slot
        FlashSaleSlot slot = new FlashSaleSlot();
        slot.setName(request.getName());
        slot.setStartDate(request.getStartDate());
        slot.setEndDate(request.getEndDate());
        slot.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        slot.setBannerImageUrl(request.getBannerImageUrl());
        FlashSaleSlot savedSlot = flashSaleSlotRepository.save(slot);
        // 5. Tạo items
        List<FlashSaleItem> itemsToSave = new ArrayList<>();
        for (FlashSaleItemRequest ir : itemRequests) {
            FlashSaleItemId itemId = new FlashSaleItemId();
            itemId.setSlotId(savedSlot.getSlotId());
            itemId.setSkuId(ir.getSkuId());
            FlashSaleItem item = new FlashSaleItem();
            item.setId(itemId);
            item.setOriginalPrice(ir.getOriginalPrice());
            item.setFlashSalePrice(ir.getFlashSalePrice());
            item.setFlashSaleQuantity(ir.getFlashSaleQuantity());
            // soldQuantity = 0 mặc định (entity set default)
            itemsToSave.add(item);
        }
        List<FlashSaleItem> savedItems = flashSaleItemRepository.saveAll(itemsToSave);
        // 6. Build response thủ công để có đầy đủ thông tin SKU (không cần load lại)
        List<FlashSaleItemResponse> itemResponses = new ArrayList<>();
        for (int i = 0; i < savedItems.size(); i++) {
            FlashSaleItem saved = savedItems.get(i);
            FlashSaleItemRequest ir = itemRequests.get(i);
            ProductSku sku = skuMap.get(saved.getId().getSkuId());
            itemResponses.add(FlashSaleItemResponse.builder()
                    .skuId(saved.getId().getSkuId())
                    .productId(sku.getProduct() != null ? sku.getProduct().getProductId() : null)
                    .productName(sku.getProduct() != null ? sku.getProduct().getProductName() : null)
                    .skuCode(sku.getSkuCode())
                    .skuImageUrl(sku.getSkuImageUrl())
                    .originalPrice(saved.getOriginalPrice())
                    .flashSalePrice(saved.getFlashSalePrice())
                    .flashSaleQuantity(saved.getFlashSaleQuantity())
                    .soldQuantity(0)
                    .remainingQuantity(saved.getFlashSaleQuantity())
                    .createdAt(saved.getCreatedAt())
                    .build());
        }
        return FlashSaleResponse.builder()
                .slotId(savedSlot.getSlotId())
                .name(savedSlot.getName())
                .startDate(savedSlot.getStartDate())
                .endDate(savedSlot.getEndDate())
                .status(savedSlot.getStatus())
                .quantityFlashSaleSlot(savedItems.stream()
                        .mapToInt(FlashSaleItem::getFlashSaleQuantity).sum())
                .usedQuantity(savedItems.stream()
                        .mapToInt(i -> i.getSoldQuantity() == null ? 0 : i.getSoldQuantity())
                        .sum())
                .createdAt(savedSlot.getCreatedAt())
                .updatedAt(savedSlot.getUpdatedAt())
                .bannerImageUrl(savedSlot.getBannerImageUrl())
                .items(itemResponses)
                .build();
    }

    // Kiểm tra khoảng thời gian có đang đụng với slot khác không.
    // FE gọi khi admin thay đổi startDate/endDate để hiện cảnh báo real-time.
    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> checkOverlappingSlots(
            LocalDateTime startDate, LocalDateTime endDate, Integer excludeSlotId) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            return List.of();
        }
        List<FlashSaleSlot> overlapping = flashSaleSlotRepository
                .findOverlappingSlots(startDate, endDate, excludeSlotId);

        // Gọi map usedMap (chi tiết) nhỏ để có thêm soldQuantity cho UI
        List<Integer> slotIds = overlapping.stream()
                .map(FlashSaleSlot::getSlotId)
                .toList();
        Map<Integer, Integer> qtyMap = slotIds.isEmpty()
                ? Map.of()
                : flashSaleItemRepository.sumFlashSaleQuantityBySlotIds(slotIds).stream()
                        .collect(Collectors.toMap(
                                row -> (Integer) row[0],
                                row -> ((Number) row[1]).intValue()));
        Map<Integer, Integer> usedMap = slotIds.isEmpty()
                ? Map.of()
                : flashSaleItemRepository.sumSoldQuantityBySlotIds(slotIds).stream()
                        .collect(Collectors.toMap(
                                row -> (Integer) row[0],
                                row -> ((Number) row[1]).intValue()));
        return overlapping.stream()
                .map(s -> toResponse(s, qtyMap, usedMap))
                .toList();
    }

    // ========================================================
    // SCHEDULED: Tự động chuyển trạng thái flash sale theo thời gian
    // Chạy mỗi 5 phút (cron: giây phút giờ ngày-tháng tháng thứ)
    // ========================================================
    @Override
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoUpdateFlashSaleStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Chuyển slot 'Lên lịch' (1) mà đã đến giờ bắt đầu → 'Đang diễn ra' (2)
        List<FlashSaleSlot> toActivate = flashSaleSlotRepository.findSlotsToActivate(now);
        if (!toActivate.isEmpty()) {
            toActivate.forEach(s -> s.setStatus((short) 2));
            flashSaleSlotRepository.saveAll(toActivate);
            log.info("[FlashSale] Đã kích hoạt {} slot: {}",
                    toActivate.size(),
                    toActivate.stream().map(FlashSaleSlot::getSlotId).toList());
        }

        // 2. Chuyển slot 'Đang diễn ra' (2) mà đã quá endDate → 'Đã kết thúc' (3)
        //    Việc chặn mua sau khi kết thúc được đảm bảo qua slot.status = 3
        //    (repository query findActiveFlashSaleItemBySku đã filter theo status).
        //    Không ghi đè flashSaleQuantity để giữ nguyên tổng ban đầu cho báo cáo
        //    và tránh vi phạm CHECK constraint flash_sale_quantity > 0.
        List<FlashSaleSlot> toExpire = flashSaleSlotRepository.findSlotsToExpire(now);
        if (!toExpire.isEmpty()) {
            toExpire.forEach(s -> s.setStatus((short) 3));
            flashSaleSlotRepository.saveAll(toExpire);

            log.info("[FlashSale] Đã kết thúc {} slot: {}",
                    toExpire.size(),
                    toExpire.stream().map(FlashSaleSlot::getSlotId).toList());
        }

        // 3. Slot 'Lên lịch' (1) mà đã quá endDate mà chưa kịp chạy → 'Đã kết thúc' (3)
        List<FlashSaleSlot> overdue = flashSaleSlotRepository.findOverdueScheduledSlots(now);
        if (!overdue.isEmpty()) {
            overdue.forEach(s -> s.setStatus((short) 3));
            flashSaleSlotRepository.saveAll(overdue);
            log.info("[FlashSale] Đã đánh dấu kết thúc {} slot bị quên: {}",
                    overdue.size(),
                    overdue.stream().map(FlashSaleSlot::getSlotId).toList());
        }
    }
}

