package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.repository.analytics.AiProductClickRepository.AiProductClickStatProjection;
import com.fpoly.marcusstore.service.ai.AiProductClickService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-advisor")
@RequiredArgsConstructor
public class AdminAiAdvisorController {

    private final AiProductClickService clickService;

    @GetMapping("/top-clicked-products")
    public ApiResponse<List<AiProductClickStatProjection>> topClickedProducts() {
        return ApiResponse.success(clickService.topProducts());
    }
}
