package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.response.ClientSuggestedProductResponse;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.analytics.AnalyticsRepository;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.shopping.ClientSuggestedProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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

        Map<Integer, double[]> ratingMap = buildRatingMap(productIds);

        List<ClientSuggestedProductResponse> result = new ArrayList<>();
        for (Product p : products) {
            result.add(buildCard(p, ratingMap));
        }
        return result;
    }

    /**
     * Top sản phẩm bán chạy: SUM(quantity) từ các đơn COMPLETED + thanh toán
     * SUCCESS, lấy theo Product hiện đang active. Tận dụng query JOIN có sẵn
     * trong AnalyticsRepository.findBestSellers.
     */
    @Transactional(readOnly = true)
    public List<ClientSuggestedProductResponse> getBestSellers(int limit) {
        int max = (limit <= 0) ? DEFAULT_LIMIT : limit;

        List<AnalyticsRepository.BestSellerProjection> bestSellers =
                analyticsRepository.findBestSellers(max);
        if (bestSellers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> productIds = new ArrayList<>();
        for (AnalyticsRepository.BestSellerProjection bs : bestSellers) {
            productIds.add(bs.getProductId());
        }

        Map<Integer, double[]> ratingMap = buildRatingMap(productIds);

        Map<Integer, AnalyticsRepository.BestSellerProjection> bsMap = new HashMap<>();
        for (AnalyticsRepository.BestSellerProjection bs : bestSellers) {
            bsMap.put(bs.getProductId(), bs);
        }

        // Lấy Product entity để build card (cần getSkus() tính giá)
        List<Product> products = productRepository.findAllById(productIds);

        List<ClientSuggestedProductResponse> result = new ArrayList<>();
        for (Product p : products) {
            if (bsMap.containsKey(p.getProductId())) {
                result.add(buildCard(p, ratingMap));
            }
        }
        return result;
    }

    /**
     * Gợi ý sản phẩm theo từ khóa (cho panel dropdown khi đang gõ). Trả về
     * danh sách phẳng, không phân trang.
     */
    @Transactional(readOnly = true)
    public List<ClientSuggestedProductResponse> suggestByKeyword(String q, int limit) {
        if (q == null || q.trim().isEmpty()) {
            return new ArrayList<>();
        }
        int max = (limit <= 0) ? DEFAULT_LIMIT : limit;
        Pageable pageable = PageRequest.of(0, max);

        List<Product> products = productRepository
                .findByProductNameContainingIgnoreCaseAndStatusTrue(q.trim(), pageable);
        if (products.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> productIds = new ArrayList<>();
        for (Product p : products) {
            productIds.add(p.getProductId());
        }
        Map<Integer, double[]> ratingMap = buildRatingMap(productIds);

        List<ClientSuggestedProductResponse> result = new ArrayList<>();
        for (Product p : products) {
            result.add(buildCard(p, ratingMap));
        }
        return result;
    }

    /**
     * Tìm kiếm đầy đủ có phân trang cho trang /search.
     * - keyword: từ khóa tên SP (LIKE ignore case)
     * - parentCategoryId / parentCategorySlug: lọc theo category cha
     *   (id=1 Điện thoại / id=6 Phụ kiện / id=10 Máy tính bảng, null = tất cả).
     *   Nếu truyền slug, sẽ lookup id tương ứng.
     * - brandId / brandSlug: lọc theo brand = id Category con
     *   (Apple/Samsung/..., null = tất cả). Nếu truyền slug, sẽ lookup id tương ứng.
     *   Chip Loại + chip Hãng là filter AND
     * - sortBy: 'price_desc' (mặc định) | 'price_asc'
     *
     * Đặc biệt: khi Loại = "Phụ kiện" (resolve ra id trùng root category
     * "Phụ kiện"), BE bỏ qua LIKE theo tên và trả về TẤT CẢ phụ kiện đang
     * bán (status=true + còn hàng) — đồng nhất với logic gợi ý phụ kiện bên
     * giỏ hàng (CartService.getCartSuggestions → findActiveAccessoriesByBrands),
     * chỉ khác là không filter theo brand trong giỏ.
     */
    @Transactional(readOnly = true)
    public Page<ClientSuggestedProductResponse> search(String q,
            Integer parentCategoryId, String parentCategorySlug,
            Integer brandId, String brandSlug,
            String sortBy,
            int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        String order = "price_desc".equalsIgnoreCase(sortBy) ? "DESC" : "ASC";

        // Lookup id từ slug nếu FE gửi slug thay vì id
        Integer resolvedParentId = resolveCategoryId(parentCategoryId, parentCategorySlug);
        Integer resolvedBrandId = resolveCategoryId(brandId, brandSlug);

        boolean hasKeyword = q != null && !q.trim().isEmpty();

        // Nhánh "Phụ kiện" + KHÔNG có keyword → lấy TẤT CẢ PK active còn hàng
        // (giống CartService.getCartSuggestions, không filter brand). Đây là
        // hành vi khi user click chip Phụ kiện trên trang Search mà chưa gõ từ khóa.
        if (isAccessoryRoot(resolvedParentId) && !hasKeyword) {
            Integer accessoryRootId = findAccessoryRootId();
            if (accessoryRootId == null) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }
            List<Product> allMatches = productRepository
                    .findActiveAccessoriesByBrands(
                            accessoryRootId,
                            allBrandNamesOfAccessories(accessoryRootId));
            return toPagedResponse(allMatches, order, pageable);
        }

        // Có keyword (kể cả khi chip Phụ kiện đang bật) → LIKE theo tên như cũ
        if (!hasKeyword) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
        List<Product> allMatches = productRepository.findByKeywordAndParentCategory(
                q.trim(), resolvedParentId, resolvedBrandId);
        return toPagedResponse(allMatches, order, pageable);
    }

    // ---- PRIVATE HELPERS ----

    /**
     * Tìm id của root category "Phụ kiện" theo tên (giống CartService, không hardcode).
     * Trả null nếu DB chưa có root tên "Phụ kiện".
     */
    private Integer findAccessoryRootId() {
        return categoryRepository.findByParentIsNullAndStatusTrue()
                .stream()
                .filter(c -> "Phụ kiện".equalsIgnoreCase(c.getCategoryName()))
                .map(c -> c.getCategoryId())
                .findFirst()
                .orElse(null);
    }

    /** Check id truyền vào có phải root "Phụ kiện" không. */
    private boolean isAccessoryRoot(Integer categoryId) {
        if (categoryId == null) return false;
        Integer rootId = findAccessoryRootId();
        return rootId != null && rootId.equals(categoryId);
    }

    /**
     * Tập tất cả brand của các SP phụ kiện đang active còn hàng — dùng cho
     * nhánh phụ kiện trên trang Search (lấy hết, không lọc brand). Tận dụng
     * query DISTINCT brand riêng để tránh load full Product entity.
     */
    private java.util.Collection<String> allBrandNamesOfAccessories(Integer accessoryRootId) {
        return productRepository.findDistinctBrandsOfActiveAccessories(accessoryRootId);
    }

    /** Sort theo giá + phân trang + build response. */
    private Page<ClientSuggestedProductResponse> toPagedResponse(
            List<Product> allMatches, String order, Pageable pageable) {
        long total = allMatches.size();
        List<Product> sorted = new ArrayList<>(allMatches);
        sorted.sort((a, b) -> {
            BigDecimal pa = minActivePrice(a);
            BigDecimal pb = minActivePrice(b);
            if (pa == null && pb == null)
                return 0;
            if (pa == null)
                return "ASC".equals(order) ? 1 : -1;
            if (pb == null)
                return "ASC".equals(order) ? -1 : 1;
            int cmp = pa.compareTo(pb);
            return "ASC".equals(order) ? cmp : -cmp;
        });
        int from = (int) Math.min((long) pageable.getOffset(), sorted.size());
        int to = Math.min(from + pageable.getPageSize(), sorted.size());
        List<Product> pageContent = sorted.subList(from, to);

        List<Integer> productIds = new ArrayList<>();
        for (Product p : pageContent) {
            productIds.add(p.getProductId());
        }
        Map<Integer, double[]> ratingMap = buildRatingMap(productIds);

        List<ClientSuggestedProductResponse> mapped = new ArrayList<>();
        for (Product p : pageContent) {
            mapped.add(buildCard(p, ratingMap));
        }
        return new PageImpl<>(mapped, pageable, total);
    }

    private BigDecimal minActivePrice(Product p) {
        if (p.getSkus() == null)
            return null;
        BigDecimal min = null;
        for (ProductSku sku : p.getSkus()) {
            if (!Boolean.TRUE.equals(sku.getIsActive()) || sku.getPrice() == null) {
                continue;
            }
            if (min == null || sku.getPrice().compareTo(min) < 0) {
                min = sku.getPrice();
            }
        }
        return min;
    }

    // ---- PRIVATE HELPERS ----

    /**
     * Tra id Category từ (id, slug). Ưu tiên id nếu truyền cả hai;
     * nếu chỉ truyền slug thì lookup DB. Trả null khi không có tham số nào
     * hoặc slug không tồn tại (để query bỏ filter).
     */
    private Integer resolveCategoryId(Integer id, String slug) {
        if (id != null) return id;
        if (slug == null || slug.isBlank()) return null;
        return categoryRepository.findBySlug(slug.trim())
                .map(c -> c.getCategoryId())
                .orElse(null);
    }

    private Map<Integer, double[]> buildRatingMap(List<Integer> productIds) {
        Map<Integer, double[]> ratingMap = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return ratingMap;
        }
        for (ClientSuggestedProductRepository.RatingAggProjection proj : suggestedProductRepository
                .findRatingAggByProductIds(productIds)) {
            ratingMap.put(proj.getProductId(), new double[] {
                    proj.getAvgRating() != null ? proj.getAvgRating() : 0.0,
                    proj.getReviewCount() != null ? proj.getReviewCount() : 0L
            });
        }
        return ratingMap;
    }

    private ClientSuggestedProductResponse buildCard(Product p, Map<Integer, double[]> ratingMap) {
        BigDecimal cheapestPrice = null;
        BigDecimal cheapestOriginalPrice = null;
        Integer defaultSkuId = null;
        boolean anyInStock = false;

        if (p.getSkus() != null) {
            for (ProductSku sku : p.getSkus()) {
                if (!Boolean.TRUE.equals(sku.getIsActive()) || sku.getPrice() == null) {
                    continue;
                }
                int stock = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
                if (stock > 0) {
                    anyInStock = true;
                }
                boolean isCandidate = defaultSkuId == null
                        || sku.getPrice().compareTo(cheapestPrice) < 0;
                if (isCandidate) {
                    cheapestPrice = sku.getPrice();
                    cheapestOriginalPrice = sku.getOriginalPrice();
                    defaultSkuId = sku.getSkuId();
                }
            }
        }

        // Nếu SKU rẻ nhất không còn hàng nhưng sản phẩm vẫn có SKU active
        // khác còn hàng → chuyển sang SKU còn hàng rẻ nhất để user click
        // là thêm được ngay, không phải điều hướng sang trang chiết tiết.
        if (p.getSkus() != null && defaultSkuId != null) {
            ProductSku currentDefault = null;
            for (ProductSku sku : p.getSkus()) {
                if (sku.getSkuId().equals(defaultSkuId)) {
                    currentDefault = sku;
                    break;
                }
            }
            if (currentDefault == null
                    || (currentDefault.getStockQuantity() == null
                            || currentDefault.getStockQuantity() <= 0)) {
                ProductSku inStockCheapest = null;
                for (ProductSku sku : p.getSkus()) {
                    if (!Boolean.TRUE.equals(sku.getIsActive()) || sku.getPrice() == null) {
                        continue;
                    }
                    int stock = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
                    if (stock <= 0) {
                        continue;
                    }
                    if (inStockCheapest == null
                            || sku.getPrice().compareTo(inStockCheapest.getPrice()) < 0) {
                        inStockCheapest = sku;
                    }
                }
                if (inStockCheapest != null) {
                    defaultSkuId = inStockCheapest.getSkuId();
                    cheapestPrice = inStockCheapest.getPrice();
                    cheapestOriginalPrice = inStockCheapest.getOriginalPrice();
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
                .defaultSkuId(defaultSkuId)
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