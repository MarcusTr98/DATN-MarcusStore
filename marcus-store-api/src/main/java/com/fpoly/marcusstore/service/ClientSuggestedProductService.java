package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ClientSuggestedProductResponse;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.shopping.ClientSuggestedProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientSuggestedProductService {

    @Autowired
    private ClientSuggestedProductRepository suggestedProductRepository;

    private static final Integer DEFAULT_SUGGESTED_CATEGORY_ID = 6; // Phụ kiện
    private static final int DEFAULT_LIMIT = 8;

    @Transactional(readOnly = true)
    public List<ClientSuggestedProductResponse> getSuggestedProducts(Integer categoryId, Integer limit) {
        Integer catId = (categoryId == null) ? DEFAULT_SUGGESTED_CATEGORY_ID : categoryId;
        int max = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        Pageable pageable = PageRequest.of(0, max);

        List<Product> products = suggestedProductRepository.findSuggestedByCategory(catId, pageable);
        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> productIds = new ArrayList<>();
        for (Product p : products) {
            productIds.add(p.getProductId());
        }

        Map<Integer, double[]> ratingMap = new HashMap<>();
        for (ClientSuggestedProductRepository.RatingAggProjection proj : suggestedProductRepository.findRatingAggByProductIds(productIds)) {
            ratingMap.put(proj.getProductId(), new double[] {
                    proj.getAvgRating() != null ? proj.getAvgRating() : 0.0,
                    proj.getReviewCount() != null ? proj.getReviewCount() : 0L
            });
        }

        List<ClientSuggestedProductResponse> result = new ArrayList<>();
        for (Product p : products) {
            result.add(buildCard(p, ratingMap));
        }
        return result;
    }

    private ClientSuggestedProductResponse buildCard(Product p, Map<Integer, double[]> ratingMap) {
        BigDecimal cheapestPrice = null;
        BigDecimal cheapestOriginalPrice = null;
        boolean anyInStock = false;

        if (p.getSkus() != null) {
            for (ProductSku sku : p.getSkus()) {
                if (!Boolean.TRUE.equals(sku.getIsActive()) || sku.getPrice() == null) {
                    continue;
                }
                if (cheapestPrice == null || sku.getPrice().compareTo(cheapestPrice) < 0) {
                    cheapestPrice = sku.getPrice();
                    cheapestOriginalPrice = sku.getOriginalPrice();
                }
                int stock = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
                if (stock > 0) {
                    anyInStock = true;
                }
            }
        }

        Integer discountPercent = calculateDiscountPercent(cheapestPrice, cheapestOriginalPrice);
        double[] ratingInfo = ratingMap.getOrDefault(p.getProductId(), new double[] { 0.0, 0.0 });

        return ClientSuggestedProductResponse.builder()
                .productId(p.getProductId())
                .productName(p.getProductName())
                .slug(p.getSlug())
                .thumbnailUrl(p.getThumbnailUrl())
                .brand(p.getBrand())
                .price(cheapestPrice)
                .originalPrice(cheapestOriginalPrice)
                .discountPercent(discountPercent)
                .inStock(anyInStock)
                .rating(ratingInfo[0] > 0 ? Math.round(ratingInfo[0] * 10.0) / 10.0 : 0.0)
                .reviewCount((long) ratingInfo[1])
                .build();
    }

    private Integer calculateDiscountPercent(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || originalPrice == null) return null;
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) return null;
        if (originalPrice.compareTo(price) <= 0) return null;
        BigDecimal diff = originalPrice.subtract(price);
        BigDecimal pct = diff.multiply(BigDecimal.valueOf(100)).divide(originalPrice, 0, RoundingMode.HALF_UP);
        return pct.intValue();
    }
}