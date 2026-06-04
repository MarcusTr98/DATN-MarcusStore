package com.fpoly.marcusstore.controller.client;

import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
 @GetMapping("/{userId}")
    public CartResponse getDetailByUserId(@PathVariable("userId") Integer userId){
     return cartService.getCartDetailByUserId(userId);
 }
}
