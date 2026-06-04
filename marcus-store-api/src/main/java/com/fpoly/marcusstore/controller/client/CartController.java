package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.request.DeleteSelectedCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/{userId}")
    public CartResponse getDetailByUserId(@PathVariable("userId") Integer userId) {
        return cartService.getCartDetailByUserId(userId);
    }

    @PostMapping("/{userId}/items")
    public CartResponse addItemToCart(@PathVariable("userId") Integer userId, @RequestBody AddCartItemRequest request) {
        return cartService.addItemToCart(userId, request);
    }

    @DeleteMapping("/{userId}/items/{skuId}")
    public CartResponse removeItemFromCart(@PathVariable("userId") Integer userId, @PathVariable("skuId") Integer skuId) {
        return cartService.removeItemFromCart(userId, skuId);
    }

    @DeleteMapping("/{userId}/items/selected")
    public CartResponse removeItemsFromCart(@PathVariable("userId") Integer userId, @RequestBody DeleteSelectedCartItemRequest request) {
        return cartService.removeItemsFromCart(userId, request.getSkuIds());
    }

    @DeleteMapping("/{userId}/items")
    public CartResponse removeCartItems(@PathVariable("userId") Integer userId) {
        return cartService.removeCartItems(userId);
    }
}

