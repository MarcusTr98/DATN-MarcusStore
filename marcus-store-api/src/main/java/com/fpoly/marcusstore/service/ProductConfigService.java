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
import java.util.List;

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
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

        // Dùng Set để chặn trùng lặp mã SKU ngay trong chính danh sách gửi lên
        java.util.Set<String> uniqueCodes = new java.util.HashSet<>();

        for (SkuBatchCreateRequest.SkuItem item : request.getSkus()) {
            // Kiểm tra trùng lặp trong payload
            if (!uniqueCodes.add(item.getSkuCode())) {
                throw new RuntimeException(
                        "Mã SKU [" + item.getSkuCode() + "] bị trùng lặp trong chính danh sách bạn đang tạo!");
            }

            // Kiểm tra trùng lặp dưới Database
            if (skuRepository.existsBySkuCode(item.getSkuCode())) {
                throw new RuntimeException(
                        "Mã SKU [" + item.getSkuCode() + "] đã tồn tại trong hệ thống. Vui lòng đổi mã khác!");
            }

            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setSkuCode(item.getSkuCode());
            sku.setPrice(item.getPrice());
            sku.setStockQuantity(item.getStock());
            sku.setWeightGram(500);
            sku.setIsActive(true);

            List<AttributeValue> attributeValues = attributeValueRepository.findAllById(item.getValueIds());
            if (attributeValues.size() != item.getValueIds().size()) {
                throw new RuntimeException("Có lỗi: Một số ID thuộc tính không tồn tại trong CSDL.");
            }
            sku.setAttributeValues(attributeValues);

            skuRepository.save(sku);
        }
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

    // 3. LẤY DANH SÁCH SKU THEO PRODUCT
    public List<ProductSku> getSkusByProductId(Integer productId) {
        return skuRepository.findByProductProductIdAndIsActiveTrue(productId);
    }

    // 4. CẬP NHẬT 1 SKU LẺ
    @Transactional
    public ProductSku updateSingleSku(Integer skuId, BigDecimal price, Integer stockQuantity) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        sku.setPrice(price);
        sku.setStockQuantity(stockQuantity);
        return skuRepository.save(sku);
    }

    // 5. XÓA MỀM SKU
    @Transactional
    public void deleteSku(Integer skuId) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        sku.setIsActive(false);
        skuRepository.save(sku);
    }
}