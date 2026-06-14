package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.request.UpdateCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartItemResponse;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.Cart;
import com.fpoly.marcusstore.entity.shopping.CartItem;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.shopping.CartItemRepository;
import com.fpoly.marcusstore.repository.shopping.CartRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository; 
    // lấy giá trị thuộc tính của SKU theo tên giá trị
    private String getSkuAttributeValue(ProductSku sku, String attributeName){
        if(sku == null || sku.getAttributeValues() == null){
            return null;
        }
        return sku.getAttributeValues().stream()
                .filter(value -> value!=null) // bỏ qua phần tử null
                .filter(value -> value.getAttribute() != null)
                .filter(value -> value.getAttribute().getAttributeName() !=null)// check sự tồn tại của thuộc tính
                .filter(value -> value.getAttribute().getAttributeName().equalsIgnoreCase(attributeName))// so sánh attributeName
                .map(AttributeValue::getValueString)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
    @Transactional
    public CartResponse getCartDetail() {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        Integer cartId = cart.getCartId();
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cartId);
        List<CartItemResponse> itemResponses = cartItems.stream().map(item -> {
        String color = getSkuAttributeValue(item.getSku(), "Màu sắc");
        String storage = getSkuAttributeValue(item.getSku(), "Dung lượng bộ nhớ");
        String variantText = "";
        if(color != null && storage != null){
            variantText = color + " / " + storage;
        }
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
                    .color(color)
                    .storage(storage)
                    .variantText(variantText)
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
    private Cart getOrCreateCart(Integer userId) {
    return cartRepository.findByUserUserId(userId)
            .orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(userRepository.getReferenceById(userId));
                newCart.setCreatedAt(LocalDateTime.now());
                return cartRepository.save(newCart);
            });
}

    @Transactional
    public CartResponse addItemToCart(AddCartItemRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(() ->
                new RuntimeException("không tìm được giỏ hàng của người dùng: " + userId));
        ProductSku sku = productSkuRepository.findBySkuId(request.getSkuId()).orElseThrow(() ->
                new RuntimeException("không tìm thy SKU phù hợp: " + request.getSkuId()));
        Integer stockQuantity = sku.getStockQuantity();
        if (stockQuantity == null || stockQuantity <= 0) {
            throw new RuntimeException("sản phẩm đã hết hàng");
        }
        Integer quantity = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId()).orElse(null);
        if (cartItem == null) {
            if (quantity > stockQuantity) {
                throw new RuntimeException("sản phẩm đã hết hàng");
            }
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setSku(sku);
            cartItem.setQuantity(quantity);
        } else {
            Integer newQuantity = cartItem.getQuantity() + quantity;
            if (newQuantity > stockQuantity) {
                throw new RuntimeException("Số lượng thêm vượt quá số lượng trong kho");
            }
            cartItem.setQuantity(newQuantity);

        }
        cartItemRepository.save(cartItem);
        return getCartDetail();
    }

    @Transactional
    public CartResponse removeItemFromCart(Integer skuId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng của người dùng"));
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), skuId).orElseThrow(()
                -> new RuntimeException("không tìm thấy sảm phẩm cần xóa"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuId(cart.getCartId(), cartItem.getSku().getSkuId());
        return getCartDetail();
    }

    @Transactional
    public CartResponse removeItemsFromCart(List<Integer> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            throw new RuntimeException("vui lòng chọn ít nhất một sản phẩm để xóa");
        }
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng phù hợp"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuIdIn(cart.getCartId(), skuIds);
        return getCartDetail();
    }

    @Transactional
    public CartResponse removeCartItems() {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()
                -> new RuntimeException("không tìm thấy giỏ hàng"));
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        return getCartDetail();
    }
    // transactional để khi đang CRUD mà bị sai thì rollback lại tránh làm sai dữ liệu
    @Transactional
    public CartResponse updateItemQuantity(Integer skuId, UpdateCartItemRequest request){
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId).orElseThrow(()-> new RuntimeException("không tìm thấy giỏ hàng"));
         if(request.getQuantity() == null || request.getQuantity() <= 0){
             throw new RuntimeException("số lượng sản phẩm phải lớn hơn 0");
         }
         CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), skuId).orElseThrow(()
                 ->new RuntimeException("khong tìm thấy sản phẩm"));
            ProductSku sku = cartItem.getSku();
            Integer stockQuantity = sku.getStockQuantity();
            if(stockQuantity == null || stockQuantity <= 0){
                throw  new RuntimeException("sản phẩm đã hết hàng");
            }
            if(request.getQuantity() > stockQuantity){
                throw new RuntimeException("số lượng đã vượt quá số lượng trong kho");
            }
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
            return getCartDetail();
    }
}
