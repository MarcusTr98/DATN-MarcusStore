package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.AddCartItemRequest;
import com.fpoly.marcusstore.dto.request.UpdateCartItemRequest;
import com.fpoly.marcusstore.dto.response.CartItemResponse;
import com.fpoly.marcusstore.dto.response.CartResponse;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
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
import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
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
    private final FlashSaleItemRepository flashSaleItemRepository;
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
            // XỬ LÝ HIỂN THỊ GIÁ FLASH SALE (CẬP NHẬT)
            BigDecimal price;
            boolean isFlashSale = false;
            LocalDateTime now = LocalDateTime.now();

            if (item.getFlashSaleSlot() != null && item.getFlashSalePrice() != null) {
                FlashSaleSlot slot = item.getFlashSaleSlot();
                boolean slotActive = slot.getStatus() != null
                        && slot.getStatus() == 2
                        && !now.isBefore(slot.getStartDate())
                        && !now.isAfter(slot.getEndDate());

                if (slotActive) {
                    // Flash Sale còn active - hiển thị giá Flash Sale
                    price = item.getFlashSalePrice();
                    isFlashSale = true;
                } else {
                    // Flash Sale đã kết thúc - hiển thị giá gốc
                    price = item.getSku().getPrice();
                    isFlashSale = false;
                }
            } else {
                // Sản phẩm thường - hiển thị giá SKU
                price = item.getSku().getPrice();
            }

            Integer quantity = item.getQuantity();
            BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(quantity));
<<<<<<< HEAD
            // Ưu tiên lấy Product.thumbnailUrl (ảnh sản phẩm cha).
            // sku.skuImageUrl trong DB vẫn giữ nguyên, không xóa.
            String thumbnailUrl = item.getSku().getProduct() != null
                    ? item.getSku().getProduct().getThumbnailUrl()
                    : null;
            if (thumbnailUrl != null && thumbnailUrl.isBlank()) thumbnailUrl = null;
=======
            String imageUrl = item.getSku().getSkuImageUrl();
            if (imageUrl == null || imageUrl.isBlank()) {
                imageUrl = item.getSku().getProduct().getThumbnailUrl();
            }
>>>>>>> 0950ca0 (dang lam do logic mua hang flashsale)

            return CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .skuId(item.getSku().getSkuId())
                    .skuCode(item.getSku().getSkuCode())
                    .productName(item.getSku().getProduct().getProductName())
                    .thumbnailUrl(thumbnailUrl)
                    .color(color)
                    .storage(storage)
                    .variantText(variantText)
                    .price(price)
                    .quantity(quantity)
                    .totalPrice(totalPrice)
                    .stockQuantity(item.getSku().getStockQuantity())
                    // Thêm thông tin Flash Sale
                    .isFlashSale(isFlashSale)
                    .originalPrice(item.getSku().getOriginalPrice())
                    .flashSaleSlotName(isFlashSale ? item.getFlashSaleSlot().getName() : null)
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
        // XỬ LÝ FLASH SALE (THÊM MỚI)
        FlashSaleItem flashSaleItem = null;
        Integer stockQuantity;

        if (request.getFlashSaleSlotId() != null) {
            // Đây là sản phẩm Flash Sale
            // Tìm FlashSaleItem dựa trên slotId và skuId
            flashSaleItem = flashSaleItemRepository
                    .findItemsBySlotIdWithSlot(request.getFlashSaleSlotId().intValue())
                    .stream()
                    .filter(item -> item.getId().getSkuId().equals(request.getSkuId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm Flash Sale này"));

            FlashSaleSlot        slot = flashSaleItem.getSlot();

            // Validate slot đang active (status = 2)
            if (slot.getStatus() == null || slot.getStatus() != 2) {
                throw new RuntimeException("Flash Sale này chưa hoặc đã hết thời gian");
            }

            // Kiểm tra thời gian
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(slot.getStartDate()) || now.isAfter(slot.getEndDate())) {
                throw new RuntimeException("Flash Sale này chưa hoặc đã hết thời gian");
            }

            // Tính số lượng còn lại = flashSaleQuantity - soldQuantity
            stockQuantity = flashSaleItem.getFlashSaleQuantity() - flashSaleItem.getSoldQuantity();
            if (stockQuantity == null || stockQuantity <= 0) {
                throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
            }
        } else {
            // Sản phẩm thường
            stockQuantity = sku.getStockQuantity();
            if (stockQuantity == null || stockQuantity <= 0) {
                throw new RuntimeException("sản phẩm đã hết hàng");
            }
        }

        Integer quantity = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();

        // Tìm cartItem - không phân biệt Flash Sale status
        CartItem cartItem = cartItemRepository
                .findByCart_CartIdAndSku_SkuId(cart.getCartId(), sku.getSkuId())
                .orElse(null);

        if (cartItem == null) {
            // Tạo mới CartItem
            if (quantity > stockQuantity) {
                throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
            }
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setSku(sku);
            cartItem.setQuantity(quantity);

            // Set Flash Sale info nếu có
            if (flashSaleItem != null) {
                cartItem.setFlashSaleSlot(flashSaleItem.getSlot());
                cartItem.setFlashSalePrice(request.getFlashSalePrice());
            }
        } else {
            // Đã tồn tại → chỉ cộng dồn số lượng
            Integer newQuantity = cartItem.getQuantity() + quantity;
            if (newQuantity > stockQuantity) {
                if (flashSaleItem != null) {
                    throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
                } else {
                    throw new RuntimeException("Số lượng thêm vượt quá số lượng trong kho");
                }
            }
            cartItem.setQuantity(newQuantity);

            // Cập nhật Flash Sale info nếu sản phẩm đang được sale
            if (flashSaleItem != null && cartItem.getFlashSaleSlot() == null) {
                cartItem.setFlashSaleSlot(flashSaleItem.getSlot());
                cartItem.setFlashSalePrice(request.getFlashSalePrice());
            }
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
            Integer stockQuantity;

            // Xử lý riêng cho sản phẩm Flash Sale
            if (cartItem.getFlashSaleSlot() != null) {
                FlashSaleSlot slot = cartItem.getFlashSaleSlot();
                LocalDateTime now = LocalDateTime.now();

                // Kiểm tra slot còn active không
                boolean slotActive = slot.getStatus() != null
                        && slot.getStatus() == 2
                        && !now.isBefore(slot.getStartDate())
                        && !now.isAfter(slot.getEndDate());

                if (!slotActive) {
                    // Slot không còn active, lấy stock quantity thường
                    stockQuantity = sku.getStockQuantity();
                } else {
                    // Slot đang active, lấy số lượng Flash Sale còn lại
                    List<FlashSaleItem> flashSaleItems = flashSaleItemRepository
                            .findItemsBySlotIdWithSlot(slot.getSlotId().intValue());
                    FlashSaleItem flashSaleItem = flashSaleItems.stream()
                            .filter(item -> item.getId().getSkuId().equals(sku.getSkuId()))
                            .findFirst()
                            .orElse(null);

                    if (flashSaleItem == null) {
                        stockQuantity = sku.getStockQuantity();
                    } else {
                        stockQuantity = flashSaleItem.getFlashSaleQuantity() - flashSaleItem.getSoldQuantity();
                        if (stockQuantity == null || stockQuantity <= 0) {
                            throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
                        }
                    }
                }
            } else {
                stockQuantity = sku.getStockQuantity();
            }

            if(stockQuantity == null || stockQuantity <= 0){
                throw  new RuntimeException("sản phẩm đã hết hàng");
            }
            if(request.getQuantity() > stockQuantity){
                // Kiểm tra xem có phải sản phẩm Flash Sale không
                if (cartItem.getFlashSaleSlot() != null) {
                    throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
                } else {
                    throw new RuntimeException("số lượng đã vượt quá số lượng trong kho");
                }
            }
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
            return getCartDetail();
    }
}
