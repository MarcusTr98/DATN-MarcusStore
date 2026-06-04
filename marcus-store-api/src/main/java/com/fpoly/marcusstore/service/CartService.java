package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.CartItemResponse;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    @Transactional(readOnly = true)
    public CartResponse getCartDetailByUserId(Integer userId) {
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(() -> new RuntimeException("không tìm thấy giỏ hàng của người dùng" + userId));
        Integer cartId = cart.getCartId();
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cartId);
        List<CartItemResponse> itemResponses = cartItems.stream().map(item -> {
            BigDecimal price = item.getSku().getPrice();
            Integer quantity = item.getQuantity();
            BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));
            String imageUrl = item.getSku().getSkuImageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                imageUrl = item.getSku().getProduct().getThumbnailUrl();
            }
            return CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .skuId(item.getSku().getSkuId())
                    .skuCode(item.getSku().getSkuCode())
                    .productName(item.getSku().getProduct().getProductName())
                    .imageUrl(imageUrl)
                    .price(price)
                    .quantity(quantity)
                    .totalPrice(totalPrice)
                    .stockQuantity(item.getSku().getStockQuantity())
                    .build();
        }).toList();
        Integer totalQuantity = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity).sum();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .userId(userId)
                .cartId(cartId)
                .items(itemResponses)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .build();

    }
}
