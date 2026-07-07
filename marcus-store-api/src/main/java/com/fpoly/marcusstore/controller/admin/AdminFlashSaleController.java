package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.request.FlashSaleSlotRequest;
import com.fpoly.marcusstore.dto.request.UpdateFlashSaleStatusRequest;
import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import com.fpoly.marcusstore.service.FlashSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminFlashSaleController {

    private final FlashSaleService flashSaleService;

    @GetMapping("/flashsales")
    public Page<FlashSaleResponse> getFlashSalePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1)
        );
        return flashSaleService.getFlashSaleSlotsPage(keyword, status, pageable);
    }

    @GetMapping("/flashsales/stats")
    public FlashSaleStatsResponse getFlashSaleStats(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short status
    ) {
        return flashSaleService.getFlashSaleStats(keyword, status);
    }

    @GetMapping("/flashsale/{slotId}")
    public FlashSaleResponse getFlashSaleById(@PathVariable("slotId") Integer slotId) {
        return flashSaleService.getFlashSaleSlotById(slotId);
    }

    @PostMapping("/flashsale")
    public ResponseEntity<FlashSaleResponse> createFlashSale(
            @RequestBody @Valid FlashSaleSlotRequest request) {
        FlashSaleResponse response = flashSaleService.createFlashSale(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Cập nhật flash sale (admin)
    @PutMapping("/flashsale/{slotId}")
    public FlashSaleResponse updateFlashSale(
            @PathVariable("slotId") Integer slotId,
            @RequestBody @Valid FlashSaleSlotRequest request) {
        return flashSaleService.updateFlashSale(slotId, request);
    }
    // Đổi trạng thái nhanh cho slot
    @PatchMapping("/flashsale/{slotId}/status")
    public FlashSaleResponse updateFlashSaleStatus(
            @PathVariable("slotId") Integer slotId,
            @RequestBody @Valid UpdateFlashSaleStatusRequest request) {
        return flashSaleService.updateFlashSaleStatus(slotId, request.getStatus());
    }

    // FE gọi khi admin nhập startDate/endDate để cảnh báo sớm.
    @GetMapping("/flashsale/check-overlap")
    public java.util.List<FlashSaleResponse> checkOverlap(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "excludeSlotId", required = false) Integer excludeSlotId) {
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startDate);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endDate);
        return flashSaleService.checkOverlappingSlots(start, end, excludeSlotId);
    }
}
