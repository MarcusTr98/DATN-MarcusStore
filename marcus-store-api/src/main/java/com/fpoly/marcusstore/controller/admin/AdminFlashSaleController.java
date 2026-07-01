package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.FlashSaleResponse;
import com.fpoly.marcusstore.service.FlashSaleService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminFlashSaleController {
    private final FlashSaleService flashSaleService;
    @GetMapping("flashsale")
    public List<FlashSaleResponse> getAllFlashSale(){
       return flashSaleService.getAllFlashSale();
    }
}
