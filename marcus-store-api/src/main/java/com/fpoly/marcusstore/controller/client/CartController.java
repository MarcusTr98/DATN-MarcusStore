package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
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
    public CartResponse getDetailByUserId(@PathVariable("userId") Integer userId){
     return cartService.getCartDetailByUserId(userId);
 }
 @PostMapping("/{userId}/items")
    public CartResponse addItemToCart(@PathVariable("userId") Integer userId, @RequestBody AddCartItemRequest request) {
     return cartService.addItemToCart(userId, request);
 }

}
