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

        for (SkuBatchCreateRequest.SkuItem item : request.getSkus()) {
            ProductSku sku = new ProductSku();
            sku.setProduct(product);
            sku.setSkuCode(item.getSkuCode());
            sku.setPrice(item.getPrice());
            sku.setStockQuantity(item.getStock());
            sku.setIsActive(true);

            List<AttributeValue> attributeValues = attributeValueRepository.findAllById(item.getValueIds());
            if (attributeValues.size() != item.getValueIds().size()) {
                throw new RuntimeException("Có lỗi: Một số ID thuộc tính không tồn tại trong CSDL.");
            }
            sku.setAttributeValues(attributeValues);

            skuRepository.save(sku); // Save từng cái — IDENTITY tự tăng đúng
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