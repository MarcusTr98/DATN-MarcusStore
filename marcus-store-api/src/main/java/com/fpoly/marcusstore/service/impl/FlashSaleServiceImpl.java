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
                // Slot CANCELLED được map từ các nơi gọi hàm này nên mặc định false.
                .isCancelled(false)
                .build();
    }

    // Tạo response rỗng cho slot CANCELLED — chỉ giữ thông tin slot, KHÔNG trả items[].
    // FE dùng để nhận biết slotId đã bị admin hủy và hiển thị modal thông báo.
    private FlashSaleResponse buildCancelledSlotResponse(FlashSaleSlot slot) {
        return FlashSaleResponse.builder()
                .slotId(slot.getSlotId())
                .name(slot.getName())
                .startDate(slot.getStartDate())
                .endDate(slot.getEndDate())
                .status(slot.getStatus())
                .quantityFlashSaleSlot(0)
                .usedQuantity(0)
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .bannerImageUrl(slot.getBannerImageUrl())
                .isCancelled(true)
                .items(Collections.emptyList())
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
                .searchFlashSaleSlots(normalizedKeyword, status, pageable);

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
                    .thumbnailUrl(resolveSkuImageUrl(sku))
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
                    .thumbnailUrl(resolveSkuImageUrl(sku))
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
                    .thumbnailUrl(resolveSkuImageUrl(sku))
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

        // 3. Chặn đổi status nếu slot đang ở trạng thái đã đóng (3/4)
        //    Slot ENDED/CANCELLED là bất biến — chỉ scheduler được phép đổi.
        //    Cho phép hủy: SCHEDULED (1) và ACTIVE (2) → CANCELLED (4)
        if (currentStatus != null && currentStatus != 1 && currentStatus != 2) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không thể đổi trạng thái của slot đang ở trạng thái '"
                            + currentStatus + "'. Chỉ cho phép hủy slot SCHEDULED (status = 1) hoặc ACTIVE (status = 2).");
        }

        // 4. Từ SCHEDULED (1) hoặc ACTIVE (2) chỉ cho đi tới CANCELLED (status = 4)
        if ((currentStatus == 1 || currentStatus == 2) && status != 4) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Chỉ cho phép hủy slot SCHEDULED hoặc ACTIVE (chuyển sang CANCELLED = 4).");
        }
        slot.setStatus(status);
        flashSaleSlotRepository.save(slot);
        log.info("[FlashSale] Admin đổi status slot #{}: {} → {}", slotId, currentStatus, status);
        return buildSlotDetailResponse(slot);
    }

    /**
     * Khôi phục Flash Sale đã bị hủy (CANCELLED) để tiếp tục chạy trong khoảng thời gian còn lại.
     *
     * Điều kiện 1: Thời gian khôi phục
     * - Chỉ được phép khôi phục khi Flash Sale vẫn còn trong thời gian hiệu lực ban đầu.
     * - Thời điểm thực hiện khôi phục phải cách thời điểm kết thúc (End Time) tối thiểu 1 tiếng.
     *
     * Điều kiện 2: Tránh trùng lặp khung giờ (No Overlapping)
     * - Kiểm tra xem có Flash Sale nào khác đang ACTIVE (2) hoặc SCHEDULED (1)
     *   trùng với khoảng thời gian còn lại [now, endDate] hay không.
     * - Nếu có bất kỳ Flash Sale nào bị trùng, không cho phép khôi phục.
     *
     * Khi khôi phục thành công, trạng thái chuyển sang ACTIVE (2).
     */
    @Override
    @Transactional
    public FlashSaleResponse restoreFlashSale(Integer slotId) {
        // 1. Tìm slot, 404 nếu không có
        FlashSaleSlot slot = flashSaleSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy flash sale với id: " + slotId));

        // 2. Validate slot phải ở trạng thái CANCELLED (4)
        Short currentStatus = slot.getStatus();
        if (currentStatus != 4) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ có thể khôi phục các flash sale đã bị hủy (CANCELLED). Trạng thái hiện tại: " + currentStatus);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = slot.getEndDate();
        LocalDateTime restoreDeadline = endDate.minusHours(1);

        // 3. Điều kiện 1: Kiểm tra thời gian khôi phục
        //    now phải < (endDate - 1 tiếng) để còn ít nhất 1 tiếng chạy
        if (!now.isBefore(restoreDeadline)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đã quá thời hạn khôi phục. Flash Sale kết thúc lúc " + endDate
                            + ", phải khôi phục trước " + restoreDeadline + " (ít nhất 1 tiếng trước khi kết thúc).");
        }

        // 4. Điều kiện 2: Kiểm tra overlap với các slot khác
        //    Tìm các slot ACTIVE (2) hoặc SCHEDULED (1) trùng với [now, endDate]
        List<FlashSaleSlot> overlappingSlots = flashSaleSlotRepository
                .findOverlappingSlotsForRestore(now, endDate, slotId);

        if (!overlappingSlots.isEmpty()) {
            String detail = overlappingSlots.stream()
                    .map(s -> String.format("#%d '%s' (%s → %s, status=%d)",
                            s.getSlotId(),
                            s.getName(),
                            s.getStartDate(),
                            s.getEndDate(),
                            s.getStatus()))
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể khôi phục do trùng khung giờ với các flash sale khác: " + detail);
        }

        // 5. Khôi phục: Chuyển status về ACTIVE (2) để tiếp tục chạy
        slot.setStatus((short) 2);
        FlashSaleSlot savedSlot = flashSaleSlotRepository.save(slot);

        log.info("[FlashSale] Admin khôi phục slot #{}: {} → ACTIVE, endDate={}, now={}",
                slotId, currentStatus, endDate, now);
        return buildSlotDetailResponse(savedSlot);
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

    // Lấy danh sách slot "đang diễn ra" + "sắp diễn ra trong vòng 2h" cho client.
    // Trả về FlashSaleResponse kèm items[], đã sắp xếp đúng thứ tự ưu tiên.
    //
    // Bổ sung: còn trả thêm các slot đã bị admin hủy (CANCELLED = 4) mà có khoảng thời gian
    // overlap với [now, now+2h]. Slot CANCELLED không có items[] và được đánh dấu isCancelled=true.
    // Mục đích: FE nhận biết slotId nào đã bị hủy để hiển thị modal thông báo khi khách tương tác.
    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleResponse> getActiveAndUpcomingFlashSaleSlots(int limit) {
        LocalDateTime now = LocalDateTime.now();
        // Cửa sổ "sắp diễn ra" trong vòng 2 tiếng tới
        LocalDateTime upcomingThreshold = now.plusHours(2);

        // 1. Lấy tất cả slot ACTIVE đang chạy + SCHEDULED sắp chạy trong 2h tới
        //    (Repository đã lọc đúng điều kiện 2 nhóm này)
        List<FlashSaleSlot> slots = flashSaleSlotRepository
                .findActiveAndUpcomingSlots(now, upcomingThreshold);

        if (slots.isEmpty()) {
            // Vẫn tiếp tục xử lý slot CANCELLED ở bước 6 bên dưới
            slots = new ArrayList<>();
        }

        // 2. Phân tách 2 nhóm để sort theo đúng ưu tiên nghiệp vụ
        List<FlashSaleSlot> activeGroup = new ArrayList<>();
        List<FlashSaleSlot> upcomingGroup = new ArrayList<>();
        for (FlashSaleSlot s : slots) {
            Short status = s.getStatus();
            if (status != null && status == 2) {
                activeGroup.add(s);
            } else if (status != null && status == 1) {
                upcomingGroup.add(s);
            }
        }

        // 3. Sắp xếp theo yêu cầu nghiệp vụ:
        //    - Nhóm ACTIVE: endDate ASC (slot kết thúc gần nhất đứng trước)
        //    - Nhóm SCHEDULED: startDate ASC (slot bắt đầu sớm nhất đứng trước)
        activeGroup.sort(Comparator.comparing(FlashSaleSlot::getEndDate));
        upcomingGroup.sort(Comparator.comparing(FlashSaleSlot::getStartDate));

        // 4. Ghép 2 nhóm: ACTIVE trước, SCHEDULED sau; áp dụng limit
        List<FlashSaleSlot> combined = new ArrayList<>(activeGroup.size() + upcomingGroup.size());
        combined.addAll(activeGroup);
        combined.addAll(upcomingGroup);
        if (limit > 0 && combined.size() > limit) {
            combined = combined.subList(0, limit);
        }

        // 5. Build response kèm items[] cho từng slot
        //    (1 query batch mỗi slot - số slot client hiển thị thường rất nhỏ)
        List<FlashSaleResponse> result = new ArrayList<>(combined.size());
        for (FlashSaleSlot slot : combined) {
            result.add(buildSlotDetailResponse(slot));
        }

        // 6. Bổ sung slot CANCELLED (đã bị admin hủy) trong khung [now, upcomingThreshold]
        //    để FE nhận biết slotId nào đã bị hủy. Không áp dụng limit.
        List<FlashSaleSlot> cancelledSlots = flashSaleSlotRepository
                .findCancelledSlotsInRange(now, upcomingThreshold);
        for (FlashSaleSlot slot : cancelledSlots) {
            result.add(buildCancelledSlotResponse(slot));
        }

        return result;
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

    /**
     * Lấy ảnh đại diện cho SKU từ Product cha (product.thumbnailUrl).
     * Giữ nguyên sku.skuImageUrl trong DB, không xóa.
     */
    private String resolveSkuImageUrl(ProductSku sku) {
        if (sku == null || sku.getProduct() == null) return null;
        String thumb = sku.getProduct().getThumbnailUrl();
        return (thumb != null && !thumb.isBlank()) ? thumb : null;
    }
}
