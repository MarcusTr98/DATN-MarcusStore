package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.HomeProductResponse;
import com.fpoly.marcusstore.repository.core.HomeProductRepository;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.HomeProductRawProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.RatingProjection;
import com.fpoly.marcusstore.repository.core.HomeProductRepository.SpecProjection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HomeProductService {
        @Autowired
        HomeProductRepository homeRepo;

        private HomeProductResponse toHomeProductResponse(HomeProductRawProjection raw, RatingProjection rating,
                        List<String> specs) {
                BigDecimal price = raw.getPrice();
                BigDecimal originalPrice = raw.getOriginalPrice();

                int discountPercent = 0;
                if (originalPrice != null && originalPrice.compareTo(price) > 0) {
                        discountPercent = originalPrice.subtract(price)
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(originalPrice, 0, RoundingMode.HALF_UP)
                                        .intValue();
                }

                return HomeProductResponse.builder()
                                .productId(raw.getProductId())
                                .productName(raw.getProductName())
                                .slug(raw.getSlug())
                                .thumbnailUrl(raw.getThumbnailUrl())
                                .skuId(raw.getSkuId())
                                .price(price)
                                .originalPrice(discountPercent > 0 ? originalPrice : null)
                                .discountPercent(discountPercent)
                                .specs(specs != null ? specs : List.of())
                                .rating(rating != null ? Math.round(rating.getAvgRating() * 10) / 10.0 : 5.0)
                                .build();
        }

        public Page<HomeProductResponse> getHomeProducts(
                        String sortBy, Integer brandCategoryId, Integer parentCategoryId,
                        BigDecimal minPrice, BigDecimal maxPrice, String valueIdsCsv, String brandIdsCsv,
                        Pageable pageable) {

                Page<HomeProductRawProjection> rawPage = homeRepo.findHomeProductRawData(
                                sortBy, brandCategoryId, parentCategoryId, minPrice, maxPrice, valueIdsCsv, brandIdsCsv, pageable);

                if (rawPage.isEmpty()) {
                        return Page.empty(pageable);
                }

                List<Integer> productIds = rawPage.getContent().stream()
                                .map(HomeProductRawProjection::getProductId)
                                .collect(Collectors.toList());

                Map<Integer, RatingProjection> ratingMap = homeRepo
                                .findRatingDataByProductIds(productIds).stream()
                                .collect(Collectors.toMap(RatingProjection::getProductId, r -> r));

                Map<Integer, List<String>> specsMap = homeRepo
                                .findSpecsByProductIds(productIds).stream()
                                .collect(Collectors.groupingBy(
                                                SpecProjection::getProductId,
                                                Collectors.mapping(SpecProjection::getValueString,
                                                                Collectors.toList())));

                return rawPage.map(raw -> toHomeProductResponse(raw, ratingMap.get(raw.getProductId()),
                                specsMap.get(raw.getProductId())));
        }
}