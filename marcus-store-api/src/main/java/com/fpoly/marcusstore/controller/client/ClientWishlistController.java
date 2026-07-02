package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.ApiResponse;
import com.fpoly.marcusstore.dto.response.ClientWishlistResponse;
import com.fpoly.marcusstore.dto.response.ClientWishlistToggleResponse;
import com.fpoly.marcusstore.service.ClientWishlistService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/wishlist")
public class ClientWishlistController {

    @Autowired
    ClientWishlistService wishlistSer;

    @GetMapping
    public ApiResponse<Page<ClientWishlistResponse>> getMyWishlist(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(wishlistSer.getMyWishlist(pageable));
    }

    @GetMapping("/ids")
    public ApiResponse<List<Integer>> getMyWishlistProductIds() {
        return ApiResponse.success(wishlistSer.getMyWishlistProductIds());
    }

    @GetMapping("/count")
    public ApiResponse<Long> getMyWishlistCount() {
        return ApiResponse.success(wishlistSer.getMyWishlistCount());
    }

    @PostMapping("/{productId}")
    public ApiResponse<ClientWishlistToggleResponse> addToWishlist(@PathVariable("productId") Integer productId) {
        return ApiResponse.success(wishlistSer.addToWishlist(productId));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<ClientWishlistToggleResponse> removeFromWishlist(@PathVariable("productId") Integer productId) {
        return ApiResponse.success(wishlistSer.removeFromWishlist(productId));
    }

    @PostMapping("/toggle/{productId}")
    public ApiResponse<ClientWishlistToggleResponse> toggleWishlist(@PathVariable("productId") Integer productId) {
        return ApiResponse.success(wishlistSer.toggleWishlist(productId));
    }
}
