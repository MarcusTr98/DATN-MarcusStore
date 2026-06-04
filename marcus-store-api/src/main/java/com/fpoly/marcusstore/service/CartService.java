package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartItemResponse;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
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
    private final ProductSkuRepository productSkuRepository;

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

    @Transactional
    public CartResponse addItemToCart(Integer userId, AddCartItemRequest request) {
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(() ->
                new RuntimeException("không tìm được giỏ hàng của người dùng: " + userId));
        ProductSku sku = productSkuRepository.findBySkuId(request.getSkuId()).orElseThrow(() ->
                new RuntimeException("không tìm thy SKU phù hợp: " + request.getSkuId()));
        Integer quantity = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId()).orElse(null);
        if (cartItem == null) {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setSku(sku);
            cartItem.setQuantity(quantity);

        } else {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        }
        cartItemRepository.save(cartItem);
        return getCartDetailByUserId(userId);
    }

    @Transactional
    public CartResponse removeItemFromCart(Integer userId, Integer skuId) {
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng của người dùng"));
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), skuId).orElseThrow(()
                -> new RuntimeException("không tìm thấy sảm phẩm cần xóa"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuId(cart.getCartId(), cartItem.getSku().getSkuId());
        return getCartDetailByUserId(userId);
    }

    @Transactional
    public CartResponse removeItemsFromCart(Integer userId, List<Integer> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            throw new RuntimeException("vui lòng chọn ít nhất một sản phẩm để xóa");
        }
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng phù hợp"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuIdIn(cart.getCartId(), skuIds);
        return getCartDetailByUserId(userId);
    }

    @Transactional
    public CartResponse removeCartItems(Integer userId) {
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng"));
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        return getCartDetailByUserId(userId);
    }
}
