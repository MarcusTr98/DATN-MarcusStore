package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ClientProductDetailResponse;
import com.fpoly.marcusstore.security.CustomUserDetails;
import com.fpoly.marcusstore.service.ClientProductDetailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/products")
public class ClientProductDetailController {

    @Autowired
    private ClientProductDetailService productDetailService;

    @GetMapping("/{slug}")
    public ApiResponse<ClientProductDetailResponse> getProductDetail(
            @PathVariable String slug,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Integer currentUserId = (userDetails != null) ? userDetails.getUserId() : null;

        ClientProductDetailResponse data = productDetailService.getProductDetailBySlug(slug, currentUserId);
        return ApiResponse.success(data);
    }
}