package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.request.DeleteSelectedCartItemRequest;
import com.fpoly.marcusstore.dto.request.UpdateCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.dto.response.HomeProductResponse;
import com.fpoly.marcusstore.dto.response.VoucherResponse;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.fpoly.marcusstore.service.CartService;
import com.fpoly.marcusstore.service.UserVoucherService;
import com.fpoly.marcusstore.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final UserVoucherService userVoucherService;
    private final CartService cartService;

    @GetMapping
    public CartResponse getDetail() {
        return cartService.getCartDetail();
    }

    @PostMapping("/items")
    public CartResponse addItemToCart(@RequestBody AddCartItemRequest request) {
        return cartService.addItemToCart(request);
    }

    @DeleteMapping("/items/{skuId}")
    public CartResponse removeItemFromCart(@PathVariable("skuId") Integer skuId) {
        return cartService.removeItemFromCart(skuId);
    }

    @DeleteMapping("/items/selected")
    public CartResponse removeItemsFromCart(@RequestBody DeleteSelectedCartItemRequest request) {
        return cartService.removeItemsFromCart(request.getSkuIds());
    }

    @DeleteMapping("/items")
    public CartResponse removeCartItems() {
        return cartService.removeCartItems();
    }

    @PutMapping("/items/{skuId}")
    public CartResponse updateCartItemsQuantity(@PathVariable("skuId") Integer skuId, @RequestBody UpdateCartItemRequest request){
        return cartService.updateItemQuantity(skuId, request);
    }

    @GetMapping("/vouchers")
    public ResponseEntity<List<VoucherResponse>> getAvailableVouchers() {
        return ResponseEntity.ok(userVoucherService.getAvailableVouchersForUser());
    }

    /**
     * Gợi ý sản phẩm cho trang Cart.
     * - Chỉ user đã đăng nhập + giỏ có SP + có parent_category → trả về list cùng parent category.
     * - Guest hoặc giỏ trống → trả về [] (giữ nguyên logic hiện tại, KHÔNG fallback trending).
     *
     * @param limit số lượng tối đa (mặc định 12)
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<HomeProductResponse>> getCartSuggestions(
            @RequestParam(defaultValue = "12") int limit) {
        // Dùng getCurrentUserPrincipal() thay vì getCurrentUserId()
        // để tránh throw exception khi guest gọi (giữ nguyên logic: guest → trả rỗng)
        Integer userId = SecurityUtils.getCurrentUserPrincipal() != null
                ? SecurityUtils.getCurrentUserPrincipal().getUserId()
                : null;
        List<HomeProductResponse> suggestions = cartService.getCartSuggestions(userId, limit);
        return ResponseEntity.ok(suggestions);
    }
}

