package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.request.DeleteSelectedCartItemRequest;
import com.fpoly.marcusstore.dto.request.UpdateCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
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
}

