package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.dto.response.FlashSaleStatsResponse;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "slotId")
        );
        return flashSaleService.getFlashSaleSlotsPage(keyword, status,pageable);
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
}
