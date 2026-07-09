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

    // Map 1 slot sang FlashSaleResponse, lấy tổng số lượng từ map batch để tránh N+1.
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

    // chuẩn hóa keyword người dùng gửi
    private String normalizeKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }

    /**
     * Kiểm tra slot có cho phép chỉnh sửa hay không.
     * <p>
     * Chỉ có 4 trạng thái trong hệ thống:
     * 1 = SCHEDULED  (Đã lên lịch) → cho sửa
     * 2 = ACTIVE     (Đang diễn ra) → khóa
     * 3 = ENDED      (Đã kết thúc)  → khóa
     * 4 = CANCELLED  (Đã hủy)       → khóa
     * <p>
     * Scheduler tự động chuyển trạng thái theo thời gian, không cần admin thao tác.
     */
    private boolean isSlotEditable(FlashSaleSlot slot) {
        Short s = slot.getStatus();
        if (s == null) return false;
        return s == 1;
    }

    /**
     * Validate toàn bộ request trước khi tạo slot.
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

        return buildSlotDetailResponse(slot);
    }

    private FlashSaleResponse buildSlotDetailResponse(FlashSaleSlot slot) {
        Integer slotId = slot.getSlotId();

        // Load items kèm SKU để build response có items[] (FE cần để render form chỉnh sửa)
        List<FlashSaleItem> items = flashSaleItemRepository.findBySlotSlotId(slotId);

        List<FlashSaleItemResponse> itemResponses = new ArrayList<>();
        for (FlashSaleItem item : items) {
            ProductSku sku = item.getSku();
            itemResponses.add(FlashSaleItemResponse.builder()
                    .skuId(item.getId().getSkuId())
                    .productId(sku != null && sku.getProduct() != null
                            ? sku.getProduct().getProductId() : null)
                    .productName(sku != null && sku.getProduct() != null
                            ? sku.getProduct().getProductName() : null)
                    .skuCode(sku != null ? sku.getSkuCode() : null)
                    .skuImageUrl(sku != null ? sku.getSkuImageUrl() : null)
                    .originalPrice(item.getOriginalPrice())
                    .flashSalePrice(item.getFlashSalePrice())
                    .flashSaleQuantity(item.getFlashSaleQuantity())
                    .soldQuantity(item.getSoldQuantity())
                    .remainingQuantity(item.getFlashSaleQuantity()
                            - (item.getSoldQuantity() == null ? 0 : item.getSoldQuantity()))
                    .createdAt(item.getCreatedAt())
                    .build());
        }

        return FlashSaleResponse.builder()
                .slotId(slot.getSlotId())
                .name(slot.getName())
                .startDate(slot.getStartDate())
                .endDate(slot.getEndDate())
                .status(slot.getStatus())
                .quantityFlashSaleSlot(items.stream()
                        .mapToInt(FlashSaleItem::getFlashSaleQuantity).sum())
                .usedQuantity(items.stream()
                        .mapToInt(i -> i.getSoldQuantity() == null ? 0 : i.getSoldQuantity())
                        .sum())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .bannerImageUrl(slot.getBannerImageUrl())
                .items(itemResponses)
                .build();
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

    // UPDATE FLASH SALE
    @Override
    @Transactional
    public FlashSaleResponse updateFlashSale(Integer slotId, FlashSaleSlotRequest request) {
        // 1. Tìm slot, 404 nếu không có
        FlashSaleSlot slot = flashSaleSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy flash sale với id: " + slotId));

        // 2. Chặn update theo trạng thái hiện tại của slot
        // chỉ trạng thái đã lên lịch mới có thể đổi trạng thái
        Short currentStatus = slot.getStatus();
        boolean isEditable = isSlotEditable(slot);
        if (!isEditable) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Flash sale đang ở trạng thái '" + slot.getStatus()
                            + "', không thể chỉnh sửa. Chỉ cho phép sửa khi slot ở trạng thái SCHEDULED (Đã lên lịch).");
        }
        boolean isRunning = currentStatus != null && currentStatus == 2;
        boolean wantToStopRunning = isRunning
                && request.getStatus() != null
                && request.getStatus() != 2;

        // 3. Validate thời gian + items (luôn chạy để giữ message lỗi nhất quán)
        //    Lưu ý: slot cũ có thể đã chạy nên KHÔNG chặn startDate ở quá khứ.
        if (request.getStartDate() == null || request.getEndDate() == null
                || !request.getEndDate().isAfter(request.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phải có ít nhất 1 sản phẩm trong Flash Sale");
        }
        if (isRunning && !wantToStopRunning) {
            // Chặn đổi thời gian
            if (!slot.getStartDate().equals(request.getStartDate())
                    || !slot.getEndDate().equals(request.getEndDate())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Flash sale đang diễn ra, không thể thay đổi thời gian bắt đầu/kết thúc");
            }
            // Chặn sửa items: bắt buộc gửi đúng danh sách SKU + giá + số lượng hiện tại
            List<FlashSaleItem> oldItemsRunning = flashSaleItemRepository.findBySlotSlotId(slotId);
            Map<Integer, FlashSaleItem> oldItemMap = oldItemsRunning.stream()
                    .collect(Collectors.toMap(it -> it.getId().getSkuId(), it -> it));

            Set<Integer> newSkuIds = new HashSet<>();
            for (FlashSaleItemRequest ir : request.getItems()) {
                if (ir.getSkuId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "SKU id không được để trống");
                }
                if (!newSkuIds.add(ir.getSkuId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "SKU id bị trùng trong request: " + ir.getSkuId());
                }
                FlashSaleItem old = oldItemMap.get(ir.getSkuId());
                if (old == null) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Flash sale đang diễn ra, không thể thêm SKU mới: " + ir.getSkuId());
                }
                if (old.getOriginalPrice().compareTo(ir.getOriginalPrice()) != 0) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Flash sale đang diễn ra, không thể thay đổi giá gốc của SKU " + ir.getSkuId());
                }
                if (old.getFlashSalePrice().compareTo(ir.getFlashSalePrice()) != 0) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Flash sale đang diễn ra, không thể thay đổi giá Flash Sale của SKU " + ir.getSkuId());
                }
                if (!old.getFlashSaleQuantity().equals(ir.getFlashSaleQuantity())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Flash sale đang diễn ra, không thể thay đổi số lượng Flash Sale của SKU "
                                    + ir.getSkuId());
                }
            }
            // Không được xoá SKU đang chạy
            Set<Integer> oldSkuIds = oldItemMap.keySet();
            if (!oldSkuIds.equals(newSkuIds)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Flash sale đang diễn ra, không thể thêm/xoá sản phẩm khỏi slot");
            }
        }

        // 5. Load SKU map từ request
        List<FlashSaleItemRequest> itemRequests = request.getItems();
        List<Integer> skuIds = itemRequests.stream()
                .map(FlashSaleItemRequest::getSkuId)
                .toList();
        List<ProductSku> skus = productSkuRepository.findBySkuIdIn(skuIds);
        Map<Integer, ProductSku> skuMap = skus.stream()
                .collect(Collectors.toMap(ProductSku::getSkuId, s -> s));

        // 6. Validate chi tiết items
        if (!(isRunning && !wantToStopRunning)) {
            Set<Integer> seenSkuIds = new HashSet<>();
            for (FlashSaleItemRequest ir : itemRequests) {
                if (ir.getSkuId() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "SKU id không được để trống");
                }
                if (!seenSkuIds.add(ir.getSkuId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "SKU id bị trùng trong request: " + ir.getSkuId());
                }
                ProductSku sku = skuMap.get(ir.getSkuId());
                if (sku == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy SKU với id: " + ir.getSkuId());
                }
                if (ir.getOriginalPrice() == null || ir.getOriginalPrice().signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Giá gốc của SKU " + ir.getSkuId() + " phải > 0");
                }
                if (ir.getFlashSalePrice() == null || ir.getFlashSalePrice().signum() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Giá Flash Sale của SKU " + ir.getSkuId() + " phải > 0");
                }
                if (ir.getFlashSalePrice().compareTo(ir.getOriginalPrice()) >= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                            "Giá Flash Sale (%s) phải nhỏ hơn giá gốc (%s) của SKU %d",
                            ir.getFlashSalePrice().toPlainString(),
                            ir.getOriginalPrice().toPlainString(), ir.getSkuId()));
                }
                if (ir.getFlashSaleQuantity() == null || ir.getFlashSaleQuantity() < 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Số lượng Flash Sale của SKU " + ir.getSkuId() + " phải >= 1");
                }
                Integer stock = sku.getStockQuantity();
                if (stock != null && ir.getFlashSaleQuantity() > stock) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                            "Số lượng Flash Sale của SKU %d vượt tồn kho (còn %d)",
                            ir.getSkuId(), stock));
                }
            }
        }

        // 7. Build map items cũ theo skuId để check soldQuantity
        //    (tránh vi phạm CHECK constraint flash_sale_quantity >= soldQuantity)
        List<FlashSaleItem> oldItems = flashSaleItemRepository.findBySlotSlotId(slotId);
        Map<Integer, Integer> oldSoldQtyBySku = oldItems.stream()
                .collect(Collectors.toMap(
                        it -> it.getId().getSkuId(),
                        it -> it.getSoldQuantity() == null ? 0 : it.getSoldQuantity()));
        for (FlashSaleItemRequest ir : itemRequests) {
            Integer soldBefore = oldSoldQtyBySku.getOrDefault(ir.getSkuId(), 0);
            if (soldBefore > 0 && ir.getFlashSaleQuantity() < soldBefore) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(
                        "SKU %d đã có %d sản phẩm được bán, không thể giảm số lượng Flash Sale xuống %d",
                        ir.getSkuId(), soldBefore, ir.getFlashSaleQuantity()));
            }
        }

        // 8. Validate overlap với slot khác
        List<FlashSaleSlot> overlapping = flashSaleSlotRepository
                .findOverlappingSlots(request.getStartDate(), request.getEndDate(), slotId);
        if (!overlapping.isEmpty()) {
            String detail = overlapping.stream()
                    .map(s -> String.format("#%d '%s' (%s → %s)",
                            s.getSlotId(), s.getName(),
                            s.getStartDate(), s.getEndDate()))
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Khung giờ đã bị trùng với các flash sale khác: " + detail);
        }

        // 9. Update field trên entity
        //    Khi đang chạy (status=2) và KHÔNG tắt đi: KHÔNG cập nhật thời gian, status giữ nguyên = 2
        if (isRunning && !wantToStopRunning) {
            slot.setName(request.getName());
            slot.setBannerImageUrl(request.getBannerImageUrl());
            // status giữ nguyên = 2, startDate/endDate giữ nguyên
        } else {
            slot.setName(request.getName());
            slot.setStartDate(request.getStartDate());
            slot.setEndDate(request.getEndDate());
            if (request.getStatus() != null) {
                slot.setStatus(request.getStatus());
            }
            slot.setBannerImageUrl(request.getBannerImageUrl());
        }

        // 10. Replace items: clear collection (orphanRemoval sẽ xoá cũ) + add mới
        if (!(isRunning && !wantToStopRunning)) {
            slot.getFlashSaleItems().clear();
            for (FlashSaleItemRequest ir : itemRequests) {
                FlashSaleItemId itemId = new FlashSaleItemId();
                itemId.setSlotId(slotId);
                itemId.setSkuId(ir.getSkuId());
                FlashSaleItem newItem = new FlashSaleItem();
                newItem.setId(itemId);
                newItem.setSlot(slot);
                newItem.setOriginalPrice(ir.getOriginalPrice());
                newItem.setFlashSalePrice(ir.getFlashSalePrice());
                newItem.setFlashSaleQuantity(ir.getFlashSaleQuantity());
                // Giữ soldQuantity cũ cho SKU đã có (bảo toàn lịch sử bán)
                Integer keepSold = oldSoldQtyBySku.get(ir.getSkuId());
                if (keepSold != null) {
                    newItem.setSoldQuantity(keepSold);
                }
                slot.getFlashSaleItems().add(newItem);
            }
        }

        FlashSaleSlot savedSlot = flashSaleSlotRepository.save(slot);

        // 11. Build response giống createFlashSale để có items[] (form chỉnh sửa render lại đúng)
        Map<Integer, ProductSku> skuMapForResponse = skuMap;
        List<FlashSaleItemResponse> itemResponses = new ArrayList<>();
        for (FlashSaleItem savedItem : savedSlot.getFlashSaleItems()) {
            ProductSku sku = skuMapForResponse.get(savedItem.getId().getSkuId());
            itemResponses.add(FlashSaleItemResponse.builder()
                    .skuId(savedItem.getId().getSkuId())
                    .productId(sku != null && sku.getProduct() != null
                            ? sku.getProduct().getProductId() : null)
                    .productName(sku != null && sku.getProduct() != null
                            ? sku.getProduct().getProductName() : null)
                    .skuCode(sku != null ? sku.getSkuCode() : null)
                    .skuImageUrl(sku != null ? sku.getSkuImageUrl() : null)
                    .originalPrice(savedItem.getOriginalPrice())
                    .flashSalePrice(savedItem.getFlashSalePrice())
                    .flashSaleQuantity(savedItem.getFlashSaleQuantity())
                    .soldQuantity(savedItem.getSoldQuantity() == null ? 0 : savedItem.getSoldQuantity())
                    .remainingQuantity(savedItem.getFlashSaleQuantity()
                            - (savedItem.getSoldQuantity() == null ? 0 : savedItem.getSoldQuantity()))
                    .createdAt(savedItem.getCreatedAt())
                    .build());
        }
        return FlashSaleResponse.builder()
                .slotId(savedSlot.getSlotId())
                .name(savedSlot.getName())
                .startDate(savedSlot.getStartDate())
                .endDate(savedSlot.getEndDate())
                .status(savedSlot.getStatus())
                .quantityFlashSaleSlot(savedSlot.getFlashSaleItems().stream()
                        .mapToInt(FlashSaleItem::getFlashSaleQuantity).sum())
                .usedQuantity(savedSlot.getFlashSaleItems().stream()
                        .mapToInt(i -> i.getSoldQuantity() == null ? 0 : i.getSoldQuantity()).sum())
                .createdAt(savedSlot.getCreatedAt())
                .updatedAt(savedSlot.getUpdatedAt())
                .bannerImageUrl(savedSlot.getBannerImageUrl())
                .items(itemResponses)
                .build();
    }

    // CANCEL / UPDATE STATUS NHANH (admin)
    @Override
    @Transactional
    public FlashSaleResponse updateFlashSaleStatus(Integer slotId, Short status) {
        // 1. Tìm slot, 404 nếu không có
        FlashSaleSlot slot = flashSaleSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy flash sale với id: " + slotId));

        // 2. Validate status mới không null và thuộc tập cho phép
        if (status == null || (status != 1 && status != 2 && status != 3 && status != 4)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status không hợp lệ: " + status + ". Chỉ chấp nhận 1/2/3/4.");
        }

        Short currentStatus = slot.getStatus();

        // 3. Chặn đổi status nếu slot đang ở trạng thái đã đóng (2/3/4)
        //    Slot ACTIVE/ENDED/CANCELLED là bất biến — chỉ scheduler được phép đổi.
        if (currentStatus != null && currentStatus != 1) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không thể đổi trạng thái của slot đang ở trạng thái '"
                            + currentStatus + "'. Chỉ cho phép hủy slot SCHEDULED (status = 1).");
        }

        // 4. Từ SCHEDULED chỉ cho đi tới CANCELLED (status = 4)
        if (currentStatus == 1 && status != 4) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ cho phép hủy slot SCHEDULED (chuyển sang CANCELLED = 4).");
        }
        slot.setStatus(status);
        flashSaleSlotRepository.save(slot);
        log.info("[FlashSale] Admin đổi status slot #{}: {} → {}", slotId, currentStatus, status);
        return buildSlotDetailResponse(slot);
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

    // SCHEDULED: Tự động chuyển trạng thái flash sale theo thời gian
    // Chạy mỗi 1 phút (cron: giây phút giờ ngày-tháng tháng thứ)
    @Override
    @Scheduled(cron = "0 * * * * *")
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

        List<FlashSaleSlot> toExpire = flashSaleSlotRepository.findSlotsToExpire(now);
        if (!toExpire.isEmpty()) {
            toExpire.forEach(s -> s.setStatus((short) 3));
            flashSaleSlotRepository.saveAll(toExpire);

            log.info("[FlashSale] Đã kết thúc {} slot: {}",
                    toExpire.size(),
                    toExpire.stream().map(FlashSaleSlot::getSlotId).toList());
        }
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
