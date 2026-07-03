package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.response.ProductCascadeResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.service.ProductCascadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductCascadeServiceImpl implements ProductCascadeService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final ProductSkuRepository productSkuRepository;
    @Override
    @Transactional(readOnly = true)
    public List<ProductCascadeResponse> getAllProductCascade(boolean includeOutOfStock) {

        // Trả về iPhone, Samsung, Xiaomi
        List<String> brands = productRepository.findAllDistinctBrands();
        // Với mỗi brand, build cây ProductCascadeResponse
        return brands.stream()
                .map(brand -> buildCascadeByBrand(brand, includeOutOfStock))
                .filter(Objects::nonNull) // loại bỏ các brands khi brand đó không có sản phẩm và trả về null
                .collect(Collectors.toList()); // gom các brands lại thành một list
    }
    // lấy cây của một brand ví dụ iphone -> iphone 15 -> iphone 15 promax 128gb....
    @Override
    @Transactional(readOnly = true)
    public ProductCascadeResponse getProductCascadeByBrand(String brand, boolean includeOutOfStock) {
        return buildCascadeByBrand(brand, includeOutOfStock);
    }
    // xây dựng cây ProductCascadeResponse cho 1 brand cụ thể.
    private ProductCascadeResponse buildCascadeByBrand(String brand, boolean includeOutOfStock) {
        // Lấy tất cả product theo brand
        List<Product> products = productRepository.findByBrandAndStatusTrue(brand);
        if (products.isEmpty()) {
            return null;
        }
        // Bước 1: gom nhóm các products theo categoryId
        // Integer là categoryId, List<Product> là các product sẽ được thêm vào sau khi map
        // groupingBy gom nhóm các đối tượng có chug thuộc tính vào một nhoms
        Map<Integer, List<Product>> productsByCategory = products.stream()
                .collect(Collectors.groupingBy(p -> p.getCategory().getCategoryId()));
        //  Bước 2: Build CategoryL2Node cho mỗi nhóm
        List<ProductCascadeResponse.CategoryL2Node> categoryNodes = new ArrayList<>();
        //đọc dữ liệu theo thứ tự Category -> Product -> SKU rồi xây dựng DTO.
        for (Map.Entry<Integer, List<Product>> entry : productsByCategory.entrySet()) {
            // Lấy category info (lấy từ product đầu tiên trong nhóm)
            Category category = entry.getValue().get(0).getCategory();
            List<Product> categoryProducts = entry.getValue();
            List<ProductCascadeResponse.SkuNode> skuNodes = new ArrayList<>();
            for (Product product : categoryProducts) {
                List<ProductSku> skus = productSkuRepository
                        .findByProductProductIdAndIsActiveTrue(product.getProductId());
                for (ProductSku sku : skus) {
                    // ====== LỌC SKU HẾT HÀNG (điểm 1) ======
                    // Nếu includeOutOfStock = false thì bỏ qua SKU có stockQuantity <= 0
                    if (!includeOutOfStock
                            && (sku.getStockQuantity() == null || sku.getStockQuantity() <= 0)) {
                        continue; // SKU hết hàng -> không thêm vào
                    }
                    String attributes = sku.getAttributeValues().stream()
                            .map(av -> av.getValueString())
                            .collect(Collectors.joining(", "));
                    skuNodes.add(ProductCascadeResponse.SkuNode.builder()
                            .skuId(sku.getSkuId())
                            .productName(product.getProductName())
                            .originalPrice(sku.getOriginalPrice())
                            .stockQuantity(sku.getStockQuantity())
                            .attributes(attributes)
                            .build());
                }
            }
            categoryNodes.add(ProductCascadeResponse.CategoryL2Node.builder()
                    .categoryId(category.getCategoryId())
                    .categoryName(category.getCategoryName())
                    .skus(skuNodes)
                    .build());
        }
        return ProductCascadeResponse.builder()
                .brand(brand)
                .categories(categoryNodes)
                .build();
    }
}
