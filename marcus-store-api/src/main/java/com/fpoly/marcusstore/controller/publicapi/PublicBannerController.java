package com.fpoly.marcusstore.controller.publicapi;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.BannerResponseDTO;
import com.fpoly.marcusstore.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// CÔNG KHAI cho phía Client 
@RestController
@RequestMapping("/api/public/banners")
public class PublicBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping
    public ApiResponse<List<BannerResponseDTO>> getPublicBanners(
            @RequestParam(required = false) String position) {
        return ApiResponse.success(bannerService.getPublicBanners(position));
    }
}