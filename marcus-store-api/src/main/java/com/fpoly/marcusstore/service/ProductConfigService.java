package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.request.SkuGenerationRequest;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.utils.SkuGeneratorUtils;

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

    @Transactional(rollbackFor = Exception.class)
    public void generateAndSaveProductSkus(SkuGenerationRequest request) {

        // 1. Chặn request quá lớn
        if (request.getAttributeValueIds().size() > 5) {
            throw new IllegalArgumentException("Hệ thống chỉ hỗ trợ tối đa 5 lớp thuộc tính cho một sản phẩm!");
        }

        // 2. Sinh tổ hợp chuỗi mã (dạng: IPHONE15-RED-128GB)
        List<List<String>> codeCombinations = SkuGeneratorUtils.generateCombinations(request.getAttributeValueCodes());

        // 3. Sinh tổ hợp ID thuộc tính
        List<List<Integer>> idCombinations = SkuGeneratorUtils.generateCombinations(request.getAttributeValueIds());

        // 4. Khởi tạo danh sách lưu Batch Insert nâng cao hiệu năng
        List<ProductSku> skusToSave = new ArrayList<>();

        // Khởi tạo đối tượng Product Proxy (chỉ set ID) để gán quan hệ khóa ngoại
        Product productRef = new Product();
        productRef.setProductId(request.getProductId());

        for (int i = 0; i < codeCombinations.size(); i++) {
            List<String> codeCombo = codeCombinations.get(i);
            List<Integer> idCombo = idCombinations.get(i);

            // Xử lý tạo mã SKU độc nhất
            String generatedSkuCode = SkuGeneratorUtils.buildSkuCode(request.getProductCode(), codeCombo);

            ProductSku sku = new ProductSku();
            sku.setProduct(productRef);
            sku.setSkuCode(generatedSkuCode);
            sku.setPrice(BigDecimal.ZERO); // Đảm bảo đúng kiểu dữ liệu BigDecimal
            sku.setStockQuantity(0); // Khớp chính xác trường stockQuantity trong Entity
            sku.setIsActive(true);

            // Map trực tiếp danh sách các AttributeValue vào SKU
            List<AttributeValue> attributeValues = new ArrayList<>();
            for (Integer attrValueId : idCombo) {
                AttributeValue valueRef = new AttributeValue();
                valueRef.setValueId(attrValueId);
                attributeValues.add(valueRef);
            }
            sku.setAttributeValues(attributeValues);
            skusToSave.add(sku);
        }

        // 5.lưu hàng loạt xuống Database.
        skuRepository.saveAll(skusToSave);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bulkUpdateSkus(SkuBulkUpdateRequest request) {
        List<ProductSku> skusToUpdate = new ArrayList<>();

        for (SkuBulkUpdateRequest.SkuUpdateItem item : request.getSkus()) {
            // Lấy SKU từ DB lên để update
            ProductSku sku = skuRepository.findById(item.getSkuId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU ID: " + item.getSkuId()));

            sku.setPrice(item.getPrice());
            sku.setStockQuantity(item.getStockQuantity());

            skusToUpdate.add(sku);
        }
        skuRepository.saveAll(skusToUpdate);
    }
}