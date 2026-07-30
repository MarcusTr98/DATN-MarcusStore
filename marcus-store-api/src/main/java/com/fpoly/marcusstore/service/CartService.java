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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fpoly.marcusstore.dto.response.HomeProductResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.RatingProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.SpecProjection;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;
    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ProductRepository productRepository;
    private final HomeProductRepository homeProductRepository;
    private final CategoryRepository categoryRepository;

    // lấy giá trị thuộc tính của SKU theo tên giá trị
    private String getSkuAttributeValue(ProductSku sku, String attributeName) {
        if (sku == null || sku.getAttributeValues() == null) {
            return null;
        }
        return sku.getAttributeValues().stream()
                .filter(value -> value != null) // bỏ qua phần tử null
                .filter(value -> value.getAttribute() != null)
                .filter(value -> value.getAttribute().getAttributeName() != null)// check sự tồn tại của thuộc tính
                .filter(value -> value.getAttribute().getAttributeName().equalsIgnoreCase(attributeName))// so sánh
                                                                                                         // attributeName
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
            if (color != null && storage != null) {
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

            // Ưu tiên lấy Product.thumbnailUrl (ảnh sản phẩm cha).
            // sku.skuImageUrl trong DB vẫn giữ nguyên, không xóa.
            String thumbnailUrl = item.getSku().getProduct() != null
                    ? item.getSku().getProduct().getThumbnailUrl()
                    : null;
            if (thumbnailUrl != null && thumbnailUrl.isBlank())
                thumbnailUrl = null;

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
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("không tìm được giỏ hàng của người dùng: " + userId));
        ProductSku sku = productSkuRepository.findBySkuId(request.getSkuId())
                .orElseThrow(() -> new RuntimeException("không tìm thy SKU phù hợp: " + request.getSkuId()));
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

            FlashSaleSlot slot = flashSaleItem.getSlot();

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

        Integer quantity = request.getQuantity();

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
                // Marcus sửa: giá Flash Sale là dữ liệu tin cậy từ database, tuyệt đối
                // không nhận giá do frontend gửi lên.
                cartItem.setFlashSalePrice(flashSaleItem.getFlashSalePrice());
            }
        } else {
            // Đã tồn tại → chỉ cộng dồn số lượng
            // Marcus sửa tại ranh giới Checkout: không cho Cart gộp hai ngữ cảnh
            // giá thường/Flash Sale vì Checkout sẽ không xác định được giá tin cậy.
            // Không thay đổi cách thành viên quản trị chương trình Flash Sale.
            boolean existingFlashSale = cartItem.getFlashSaleSlot() != null;
            boolean requestedFlashSale = flashSaleItem != null;
            Integer existingSlotId = existingFlashSale ? cartItem.getFlashSaleSlot().getSlotId() : null;
            Integer requestedSlotId = requestedFlashSale ? flashSaleItem.getSlot().getSlotId() : null;
            if (existingFlashSale != requestedFlashSale
                    || (existingFlashSale && !existingSlotId.equals(requestedSlotId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Sản phẩm thường và sản phẩm Flash Sale phải được thêm riêng.|CART_PRICE_CONTEXT_CONFLICT");
            }

            long calculatedQuantity = (long) cartItem.getQuantity() + quantity;
            if (calculatedQuantity > Integer.MAX_VALUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Số lượng sản phẩm không hợp lệ.|INVALID_QUANTITY");
            }
            Integer newQuantity = (int) calculatedQuantity;
            if (newQuantity > stockQuantity) {
                if (flashSaleItem != null) {
                    throw new RuntimeException("Đã hết số lượng Sale, vui lòng chọn sản phẩm khác");
                } else {
                    throw new RuntimeException("Số lượng thêm vượt quá số lượng trong kho");
                }
            }
            cartItem.setQuantity(newQuantity);

            // Cập nhật Flash Sale info nếu sản phẩm đang được sale
            if (flashSaleItem != null) {
                cartItem.setFlashSalePrice(flashSaleItem.getFlashSalePrice());
            }
        }

        cartItemRepository.save(cartItem);

        return getCartDetail();
    }

    @Transactional
    public CartResponse removeItemFromCart(Integer skuId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy giỏ hàng của người dùng"));
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), skuId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy sảm phẩm cần xóa"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuId(cart.getCartId(), cartItem.getSku().getSkuId());
        return getCartDetail();
    }

    @Transactional
    public CartResponse removeItemsFromCart(List<Integer> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            throw new RuntimeException("vui lòng chọn ít nhất một sản phẩm để xóa");
        }
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy giỏ hàng phù hợp"));
        cartItemRepository.deleteByCart_CartIdAndSku_SkuIdIn(cart.getCartId(), skuIds);
        return getCartDetail();
    }

    @Transactional
    public CartResponse removeCartItems() {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy giỏ hàng"));
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        return getCartDetail();
    }

    // transactional để khi đang CRUD mà bị sai thì rollback lại tránh làm sai dữ
    // liệu
    @Transactional
    public CartResponse updateItemQuantity(Integer skuId, UpdateCartItemRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> new RuntimeException("không tìm thấy giỏ hàng"));
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("số lượng sản phẩm phải lớn hơn 0");
        }
        CartItem cartItem = cartItemRepository.findByCart_CartIdAndSku_SkuId(cart.getCartId(), skuId)
                .orElseThrow(() -> new RuntimeException("khong tìm thấy sản phẩm"));
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

        if (stockQuantity == null || stockQuantity <= 0) {
            throw new RuntimeException("sản phẩm đã hết hàng");
        }
        if (request.getQuantity() > stockQuantity) {
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

    // logic lấy phụ kiẹn gợi ý theo các sản phẩm trong giỏ hàng
    @Transactional(readOnly = true)
    public List<HomeProductResponse> getCartSuggestions(Integer userId, int limit) {
        // 1. Guest -> tra rong
        if (userId == null) {
            return List.of();
        }

        // 2. Lay cart, neu chua co hoac rong -> tra rong (giu logic cu)
        Cart cart = cartRepository.findByUserUserId(userId).orElse(null);
        if (cart == null) {
            return List.of();
        }
        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());
        if (cartItems == null || cartItems.isEmpty()) {
            return List.of();
        }

        // 3. Gom brand tu cac SP dang co trong gio
        Set<String> cartBrands = new LinkedHashSet<>();
        Set<Integer> currentProductIds = new HashSet<>();
        for (CartItem ci : cartItems) {
            ProductSku sku = ci.getSku();
            if (sku == null || sku.getProduct() == null) {
                continue;
            }
            Product p = sku.getProduct();
            currentProductIds.add(p.getProductId());
            if (p.getBrand() != null && !p.getBrand().isBlank()) {
                cartBrands.add(p.getBrand());
            }
        }

        if (cartBrands.isEmpty()) {
            return List.of();
        }

        // 4. Tim root category "Phụ kiện" theo tên (linh hoạt, không hardcode id)
        Optional<Category> accessoryRoot = categoryRepository
                .findByParentIsNullAndStatusTrue()
                .stream()
                .filter(c -> "Phụ kiện".equalsIgnoreCase(c.getCategoryName()))
                .findFirst();

        if (accessoryRoot.isEmpty()) {
            return List.of();
        }
        Integer accessoryRootId = accessoryRoot.get().getCategoryId();

        // 5. Query phu kien cung brand (active + con hang)
        List<Product> candidates = productRepository
                .findActiveAccessoriesByBrands(accessoryRootId, cartBrands);

        // 6. Loai tru SP dang co trong gio + shuffle + cat theo limit
        List<Product> related = candidates.stream()
                .filter(p -> !currentProductIds.contains(p.getProductId()))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(related);
        if (related.size() > limit) {
            related = new ArrayList<>(related.subList(0, limit));
        }
        if (related.isEmpty()) {
            return List.of();
        }

        // 7. Map sang HomeProductResponse (giu nguyen logic mapping cu)
        List<Integer> productIds = related.stream().map(Product::getProductId).toList();

        // SKU rẻ nhất cho mỗi SP
        List<HomeProductRepository.HomeProductRawProjection> rawSkus = homeProductRepository
                .findSkuOverviewByProductIds(productIds);
        Map<Integer, HomeProductRepository.HomeProductRawProjection> skuMap = rawSkus.stream()
                .collect(Collectors.toMap(
                        HomeProductRepository.HomeProductRawProjection::getProductId,
                        r -> r,
                        (a, b) -> a));

        // Rating
        Map<Integer, Double> ratingMap = homeProductRepository
                .findRatingDataByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        RatingProjection::getProductId,
                        RatingProjection::getAvgRating));

        // Specs
        Map<Integer, List<String>> specsMap = homeProductRepository
                .findSpecsByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(
                        SpecProjection::getProductId,
                        Collectors.mapping(SpecProjection::getValueString,
                                Collectors.toList())));

        return related.stream().map(p -> {
            HomeProductRepository.HomeProductRawProjection raw = skuMap.get(p.getProductId());
            BigDecimal price = raw != null && raw.getPrice() != null
                    ? raw.getPrice()
                    : BigDecimal.ZERO;
            BigDecimal originalPrice = raw != null ? raw.getOriginalPrice() : null;
            int discountPercent = 0;
            if (originalPrice != null && originalPrice.compareTo(price) > 0) {
                discountPercent = originalPrice.subtract(price)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(originalPrice, 0, RoundingMode.HALF_UP)
                        .intValue();
            }
            List<String> specs = specsMap.getOrDefault(p.getProductId(), List.of());
            Double rating = ratingMap.get(p.getProductId());

            return HomeProductResponse.builder()
                    .productId(p.getProductId())
                    .productName(p.getProductName())
                    .slug(p.getSlug())
                    .thumbnailUrl(p.getThumbnailUrl())
                    .skuId(raw != null ? raw.getSkuId() : null)
                    .price(price)
                    .originalPrice(discountPercent > 0 ? originalPrice : null)
                    .discountPercent(discountPercent)
                    .specs(specs)
                    .rating(rating != null ? Math.round(rating * 10) / 10.0 : 5.0)
                    .build();
        }).toList();
    }
}
