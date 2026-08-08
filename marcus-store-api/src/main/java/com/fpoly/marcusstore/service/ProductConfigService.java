package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductConfigService {

    @Autowired
    private ProductSkuRepository skuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AttributeValueRepository attributeValueRepository;

    // 1. LƯU MA TRẬN SKU TỪ FRONTEND
    @Transactional(rollbackFor = Exception.class)
    public void batchCreateSkus(SkuBatchCreateRequest request) {
        Product product = productRepository.findByIdForSkuGeneration(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        // Marcus sửa: kiểm tra toàn bộ batch trước khi ghi để người dùng nhận lỗi
        // rõ ràng và không sinh nửa ma trận SKU.
        Set<String> uniqueCodes = new HashSet<>();
        Set<String> uniqueCombinations = new HashSet<>();
        Set<String> existingCombinations = skuRepository.findByProductProductId(request.getProductId()).stream()
                .map(sku -> combinationKey(sku.getAttributeValues()))
                .collect(Collectors.toSet());
        List<ProductSku> candidates = new ArrayList<>();

        for (SkuBatchCreateRequest.SkuItem item : request.getSkus()) {
            String normalizedCode = item.getSkuCode().trim().toUpperCase();
            if (!uniqueCodes.add(normalizedCode)) {
                throw new RuntimeException(
                        "Mã SKU [" + normalizedCode + "] bị trùng lặp trong chính danh sách bạn đang tạo!");
            }
            if (skuRepository.existsBySkuCodeIgnoreCase(normalizedCode)) {
                throw new RuntimeException(
                        "Mã SKU [" + normalizedCode + "] đã tồn tại trong hệ thống. Vui lòng đổi mã khác!");
            }

            List<Integer> distinctValueIds = item.getValueIds().stream().distinct().toList();
            if (distinctValueIds.size() != item.getValueIds().size()) {
                throw new IllegalArgumentException("SKU [" + normalizedCode + "] có giá trị thuộc tính bị lặp.");
            }
            List<AttributeValue> attributeValues = attributeValueRepository.findAllById(distinctValueIds);
            if (attributeValues.size() != distinctValueIds.size()) {
                throw new RuntimeException("SKU [" + normalizedCode + "] chứa ID giá trị thuộc tính không tồn tại.");
            }
            Map<Integer, Long> valuesPerAttribute = attributeValues.stream().collect(Collectors.groupingBy(
                    value -> value.getAttribute().getAttributeId(), HashMap::new, Collectors.counting()));
            if (valuesPerAttribute.values().stream().anyMatch(count -> count > 1)) {
                throw new IllegalArgumentException(
                        "SKU [" + normalizedCode + "] chỉ được chọn một giá trị cho mỗi thuộc tính.");
            }
            String combination = combinationKey(attributeValues);
            if (!uniqueCombinations.add(combination)) {
                throw new IllegalArgumentException("Tổ hợp biến thể của SKU [" + normalizedCode
                        + "] bị trùng trong danh sách đang tạo.");
            }
            if (existingCombinations.contains(combination)) {
                throw new IllegalArgumentException("Tổ hợp biến thể của SKU [" + normalizedCode
                        + "] đã tồn tại cho sản phẩm này.");
            }
            if (item.getOriginalPrice() != null && item.getOriginalPrice().compareTo(item.getPrice()) < 0) {
                throw new IllegalArgumentException("Giá gốc của SKU [" + normalizedCode
                        + "] không được nhỏ hơn giá bán.");
            }

            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setSkuCode(normalizedCode);
            sku.setPrice(item.getPrice());
            if (item.getOriginalPrice() == null) {
                sku.setOriginalPrice(item.getPrice());
            } else {
                sku.setOriginalPrice(item.getOriginalPrice());
            }

            sku.setStockQuantity(item.getStock());
            sku.setWeightGram(500);
            sku.setIsActive(true);

            sku.setAttributeValues(attributeValues);
            candidates.add(sku);
        }
        skuRepository.saveAll(candidates);
    }

    // 2. CẬP NHẬT HÀNG LOẠT (Giá, Tồn kho)
    @Transactional(rollbackFor = Exception.class)
    public void bulkUpdateSkus(SkuBulkUpdateRequest request) {
        List<ProductSku> skusToUpdate = new ArrayList<>();
        for (SkuBulkUpdateRequest.SkuUpdateItem item : request.getSkus()) {
            ProductSku sku = skuRepository.findById(item.getSkuId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU ID: " + item.getSkuId()));
            sku.setPrice(item.getPrice());
            sku.setStockQuantity(item.getStockQuantity());
            skusToUpdate.add(sku);
        }
        skuRepository.saveAll(skusToUpdate);
    }

    public List<ProductSku> getSkusByProductId(Integer productId) {
        return skuRepository.findByProductProductIdAndIsActiveTrue(productId);
    }

    @Transactional
    public ProductSku updateSingleSku(Integer skuId, BigDecimal price, Integer stockQuantity) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        sku.setPrice(price);
        sku.setStockQuantity(stockQuantity);
        return skuRepository.save(sku);
    }

    @Transactional
    public void deleteSku(Integer skuId) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        sku.setIsActive(false);
        skuRepository.save(sku);
    }

    private String combinationKey(List<AttributeValue> values) {
        return values.stream()
                .map(AttributeValue::getValueId)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }
}
