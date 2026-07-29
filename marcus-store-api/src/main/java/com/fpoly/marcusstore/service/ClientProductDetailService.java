package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ClientProductDetailResponse;
import com.fpoly.marcusstore.dto.response.ProductImgResponse;
import com.fpoly.marcusstore.dto.response.ClientProductSkuDetailResponse;
import com.fpoly.marcusstore.dto.response.ClientProductSpecValueResponse;
import com.fpoly.marcusstore.dto.response.ClientSkuAttributeValueResponse;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductImage;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.core.ProductSpecValue;
import com.fpoly.marcusstore.repository.shopping.ClientProductDetailRepository;
import com.fpoly.marcusstore.repository.shopping.OrderItemRepository;
import com.fpoly.marcusstore.repository.shopping.WishlistRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientProductDetailService {
    @Autowired
    private ClientProductDetailRepository productDetailRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private static final int LOW_STOCK_THRESHOLD = 5;

    @Transactional(readOnly = true)
    public ClientProductDetailResponse getProductDetailBySlug(String slug, Integer currentUserId) {
        Product product = productDetailRepository.findBySlugAndStatusTrue(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với slug: " + slug));

        productDetailRepository.findBySlugWithImages(slug).ifPresent(p -> {
            if (p.getImages() != null) product.setImages(p.getImages());
        });
        productDetailRepository.findBySlugWithSkus(slug).ifPresent(p -> {
            if (p.getSkus() != null) product.setSkus(p.getSkus());
        });
        
        productDetailRepository.findSkuAttributeValuesByProductSlug(slug);

        productDetailRepository.findBySlugWithSpecValues(slug).ifPresent(p -> {
            if (p.getSpecValues() != null) product.setSpecValues(p.getSpecValues());
        });

        ClientProductDetailResponse res = ClientProductDetailResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .slug(product.getSlug())
                .brand(product.getBrand())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .status(product.getStatus())
                .build();

        res.setMetaTitle(product.getProductName());
        res.setMetaDescription(product.getDescription() != null
                ? truncate(product.getDescription(), 160)
                : null);
        res.setMetaKeywords(product.getBrand());

        Category cat = product.getCategory();
        if (cat != null) {
            res.setCategoryId(cat.getCategoryId());
            res.setCategoryName(cat.getCategoryName());
            res.setCategorySlug(cat.getSlug());
            Category parent = cat.getParent();
            if (parent != null) {
                res.setParentCategoryId(parent.getCategoryId());
                res.setParentCategoryName(parent.getCategoryName());
                res.setParentCategorySlug(parent.getSlug());
            }
        }

        List<ProductImgResponse> images = new ArrayList<>();
        if (product.getImages() != null) {
            product.getImages().stream()
                    .sorted(Comparator.comparing(
                            ProductImage::getDisplayOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(img -> images.add(ProductImgResponse.builder()
                            .imageId(img.getImageId())
                            .imageUrl(img.getImageUrl())
                            .isPrimary(img.getIsPrimary())
                            .displayOrder(img.getDisplayOrder())
                            .build()));
        }
        res.setImages(images);

        List<ClientProductSkuDetailResponse> skuResponses = new ArrayList<>();
        int totalSkusActive = 0;
        int totalStock = 0;
        BigDecimal minPrice = null, maxPrice = null;
        BigDecimal minOriginalPrice = null, maxOriginalPrice = null;
        Integer minDiscount = null, maxDiscount = null;

        if (product.getSkus() != null) {
            for (ProductSku sku : product.getSkus()) {
                ClientProductSkuDetailResponse skuRes = buildSkuResponse(sku);
                skuResponses.add(skuRes);

                if (Boolean.TRUE.equals(sku.getIsActive())) {
                    totalSkusActive++;
                }
                if (sku.getStockQuantity() != null) {
                    totalStock += sku.getStockQuantity();
                }

                if (Boolean.TRUE.equals(sku.getIsActive()) && sku.getPrice() != null) {
                    BigDecimal p = sku.getPrice();
                    minPrice = (minPrice == null || p.compareTo(minPrice) < 0) ? p : minPrice;
                    maxPrice = (maxPrice == null || p.compareTo(maxPrice) > 0) ? p : maxPrice;

                    if (sku.getOriginalPrice() != null) {
                        BigDecimal op = sku.getOriginalPrice();
                        minOriginalPrice = (minOriginalPrice == null || op.compareTo(minOriginalPrice) < 0) ? op
                                : minOriginalPrice;
                        maxOriginalPrice = (maxOriginalPrice == null || op.compareTo(maxOriginalPrice) > 0) ? op
                                : maxOriginalPrice;
                    }
                    Integer disc = calculateDiscountPercent(sku.getPrice(), sku.getOriginalPrice());
                    if (disc != null) {
                        minDiscount = (minDiscount == null || disc < minDiscount) ? disc : minDiscount;
                        maxDiscount = (maxDiscount == null || disc > maxDiscount) ? disc : maxDiscount;
                    }
                }
            }
        }
        res.setSkus(skuResponses);
        res.setTotalSkus(totalSkusActive);
        res.setTotalStock(totalStock);
        res.setMinPrice(minPrice);
        res.setMaxPrice(maxPrice);
        res.setMinOriginalPrice(minOriginalPrice);
        res.setMaxOriginalPrice(maxOriginalPrice);
        res.setMinDiscountPercent(minDiscount);
        res.setMaxDiscountPercent(maxDiscount);

        Long totalSold = orderItemRepository.sumSoldQuantityByProductId(product.getProductId());
        res.setTotalSold(totalSold != null ? totalSold : 0L);

        boolean isWished = false;
        if (currentUserId != null && product.getProductId() != null) {
            isWished = wishlistRepository.existsByUserUserIdAndProductProductId(currentUserId, product.getProductId());
        }
        res.setIsWished(isWished);

        Integer pid = product.getProductId();
        System.out.println("========== PRODUCT ==========");
System.out.println("Product ID = " + pid);
        ClientProductDetailRepository.RatingSummaryProjection ratingSummary = productDetailRepository.findRatingSummaryByProductId(pid);
        System.out.println("Summary = " + ratingSummary);

if (ratingSummary != null) {
    System.out.println("AVG = " + ratingSummary.getAvgRating());
    System.out.println("COUNT = " + ratingSummary.getReviewCount());
}
        double avgRating = ratingSummary != null && ratingSummary.getAvgRating() != null
                ? ratingSummary.getAvgRating()
                : 0.0;
        long reviewCount = ratingSummary != null && ratingSummary.getReviewCount() != null
                ? ratingSummary.getReviewCount()
                : 0L;

        java.util.Map<Integer, Long> distMap = new java.util.HashMap<>();
        for (ClientProductDetailRepository.RatingDistributionProjection p : productDetailRepository.findRatingDistributionByProductId(pid)) {
            distMap.put(p.getStar(), p.getCount());
        }
        List<java.util.Map<String, Object>> distribution = new java.util.ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            java.util.Map<String, Object> row = new java.util.HashMap<>();
            row.put("star", star);
            row.put("count", distMap.getOrDefault(star, 0L));
            distribution.add(row);
        }

        res.setReviewCount(reviewCount);
        res.setRating(reviewCount > 0 ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        res.setReviewDistribution(distribution);

        List<ClientProductSpecValueResponse> specs = buildSpecResponses(product);
        res.setSpecifications(specs);

        return res;
    }

    private ClientProductSkuDetailResponse buildSkuResponse(ProductSku sku) {
        boolean active = Boolean.TRUE.equals(sku.getIsActive());
        int stock = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
        boolean inStock = active && stock > 0;
        boolean lowStock = inStock && stock <= LOW_STOCK_THRESHOLD;

        Integer discountPercent = calculateDiscountPercent(sku.getPrice(), sku.getOriginalPrice());

        List<ClientSkuAttributeValueResponse> attrs = new ArrayList<>();
        if (sku.getAttributeValues() != null) {
            for (AttributeValue av : sku.getAttributeValues()) {
                String attrName = av.getAttribute() != null ? av.getAttribute().getAttributeName() : null;
                attrs.add(ClientSkuAttributeValueResponse.builder()
                        .valueId(av.getValueId())
                        .attributeId(av.getAttribute() != null ? av.getAttribute().getAttributeId() : null)
                        .attributeName(attrName)
                        .valueString(av.getValueString())
                        .valueMeta(av.getValueMeta())
                        .build());
            }
        }

        return ClientProductSkuDetailResponse.builder()
                .skuId(sku.getSkuId())
                .skuCode(sku.getSkuCode())
                .skuImageUrl(sku.getSkuImageUrl())
                .price(sku.getPrice())
                .originalPrice(sku.getOriginalPrice())
                .discountPercent(discountPercent)
                .weightGram(sku.getWeightGram())
                .stockQuantity(stock)
                .isActive(sku.getIsActive())
                .inStock(inStock)
                .lowStock(lowStock)
                .attributeValues(attrs)
                .build();
    }

    private Integer calculateDiscountPercent(BigDecimal price, BigDecimal originalPrice) {
        if (price == null || originalPrice == null)
            return null;
        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0)
            return null;
        if (originalPrice.compareTo(price) <= 0)
            return null;
        BigDecimal diff = originalPrice.subtract(price);
        BigDecimal pct = diff.multiply(BigDecimal.valueOf(100))
                .divide(originalPrice, 0, RoundingMode.HALF_UP);
        return pct.intValue();
    }

    private String truncate(String s, int max) {
        if (s == null)
            return null;
        if (s.length() <= max)
            return s;
        return s.substring(0, max - 3) + "...";
    }

    private List<ClientProductSpecValueResponse> buildSpecResponses(Product product) {
        if (product.getSpecValues() == null || product.getSpecValues().isEmpty()) {
            return new ArrayList<>();
        }
        return product.getSpecValues().stream()
                .map(this::mapToSpecResponse)
                .collect(Collectors.toList());
    }

    private ClientProductSpecValueResponse mapToSpecResponse(ProductSpecValue psv) {
        return ClientProductSpecValueResponse.builder()
                .specAttributeId(psv.getSpecAttribute() != null ? psv.getSpecAttribute().getSpecAttributeId() : null)
                .specAttributeName(psv.getSpecAttribute() != null ? psv.getSpecAttribute().getName() : null)
                .unit(psv.getSpecAttribute() != null ? psv.getSpecAttribute().getUnit() : null)
                .dataType(psv.getSpecAttribute() != null ? psv.getSpecAttribute().getDataType() : null)
                .valueText(psv.getValueText())
                .build();
    }
}