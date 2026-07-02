package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ClientWishlistResponse;
import com.fpoly.marcusstore.dto.response.ClientWishlistToggleResponse;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.shopping.Wishlist;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.HomeProductRawProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.RatingProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.SpecProjection;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.shopping.WishlistRepository;
import com.fpoly.marcusstore.security.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClientWishlistService {

    @Autowired
    WishlistRepository wishlistRepository;

    @Autowired
    ProductRepository productRepository;
    
    @Autowired 
    UserRepository userRepository;

    @Autowired
    HomeProductRepository homeProductRepository;

    private ClientWishlistResponse toWishlistResponse(
            Wishlist w,
            HomeProductRawProjection raw,
            RatingProjection rating,
            List<String> specs) {
        Integer productId = w.getProduct().getProductId();

        BigDecimal price = raw != null ? raw.getPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = raw != null ? raw.getOriginalPrice() : null;
        Integer skuId = raw != null ? raw.getSkuId() : null;

        int discountPercent = 0;
        if (originalPrice != null && originalPrice.compareTo(price) > 0) {
            discountPercent = originalPrice.subtract(price)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, RoundingMode.HALF_UP)
                    .intValue();
        }

        return ClientWishlistResponse.builder()
                .wishlistId(w.getWishlistId())
                .productId(productId)
                .productName(w.getProduct().getProductName())
                .slug(w.getProduct().getSlug())
                .thumbnailUrl(w.getProduct().getThumbnailUrl())
                .skuId(skuId)
                .price(price)
                .originalPrice(discountPercent > 0 ? originalPrice : null)
                .discountPercent(discountPercent)
                .rating(rating != null ? Math.round(rating.getAvgRating() * 10) / 10.0 : 5.0)
                .specs(specs)
                .createdAt(w.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<ClientWishlistResponse> getMyWishlist(Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();

        Page<Wishlist> wishlistPage = wishlistRepository.findMyWishlistPage(userId, pageable);
        if (wishlistPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Wishlist> wishlists = wishlistPage.getContent();
        List<Integer> productIds = wishlists.stream()
                .map(w -> w.getProduct().getProductId())
                .toList();

        List<HomeProductRawProjection> raws = homeProductRepository
                .findSkuOverviewByProductIds(productIds);

        Map<Integer, RatingProjection> ratingMap = homeProductRepository
                .findRatingDataByProductIds(productIds).stream()
                .collect(Collectors.toMap(RatingProjection::getProductId, r -> r));

        Map<Integer, List<String>> specsMap = homeProductRepository
                .findSpecsByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(
                        SpecProjection::getProductId,
                        Collectors.mapping(SpecProjection::getValueString, Collectors.toList())));

        Map<Integer, HomeProductRawProjection> rawMap = raws.stream()
                .collect(Collectors.toMap(HomeProductRawProjection::getProductId, r -> r));

        List<ClientWishlistResponse> items = wishlists.stream()
                .filter(w -> Boolean.TRUE.equals(w.getProduct().getStatus()))
                .map(w -> toWishlistResponse(w,
                        rawMap.get(w.getProduct().getProductId()),
                        ratingMap.get(w.getProduct().getProductId()),
                        specsMap.getOrDefault(w.getProduct().getProductId(), List.of())))
                .toList();

        return new PageImpl<>(items, pageable, wishlistPage.getTotalElements());
    }


    @Transactional(readOnly = true)
    public List<Integer> getMyWishlistProductIds() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return wishlistRepository.findProductIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long getMyWishlistCount() {
        Integer userId = SecurityUtils.getCurrentUserId();
        return wishlistRepository.countByUserUserId(userId);
    }

    @Transactional
    public ClientWishlistToggleResponse addToWishlist(Integer productId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (wishlistRepository.existsByUserUserIdAndProductProductId(userId, productId)) {
            throw new RuntimeException("Sản phẩm đã có trong danh sách yêu thích");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + productId));

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(userRepository.getReferenceById(userId));
        wishlist.setProduct(product);
        wishlistRepository.save(wishlist);

        return ClientWishlistToggleResponse.builder()
                .productId(productId)
                .wished(true)
                .countWislist(wishlistRepository.countByUserUserId(userId))
                .build();
    }

    @Transactional
    public ClientWishlistToggleResponse removeFromWishlist(Integer productId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        long deleted = wishlistRepository.deleteByUserUserIdAndProductProductId(userId, productId);
        if (deleted == 0) {
            throw new RuntimeException("Sản phẩm không có trong danh sách yêu thích");
        }
        return ClientWishlistToggleResponse.builder()
                .productId(productId)
                .wished(false)
                .countWislist(wishlistRepository.countByUserUserId(userId))
                .build();
    }

    @Transactional
    public ClientWishlistToggleResponse toggleWishlist(Integer productId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        boolean existed = wishlistRepository.existsByUserUserIdAndProductProductId(userId, productId);
        if (existed) {
            return removeFromWishlist(productId);
        }
        return addToWishlist(productId);
    }
}