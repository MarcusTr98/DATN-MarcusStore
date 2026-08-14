package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.dto.response.SkuImageUpdateResponse;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class ProductConfigService {

    @Autowired
    private ProductSkuRepository skuRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AttributeValueRepository attributeValueRepository;

    @Autowired
    private FlashSaleItemRepository flashSaleItemRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

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

            // Marcus sửa sau khi tích hợp module kho: SKU mới luôn bắt đầu từ 0.
            // Mọi đơn vị hàng phải đi qua Nhập kho để số lượng và IMEI cùng một luồng.
            sku.setStockQuantity(0);
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
        // Marcus sửa: khóa toàn bộ SKU theo thứ tự ID, kiểm tra đủ batch rồi mới
        // ghi. Không cập nhật tồn kho tại luồng giá vì kho/IMEI là nguồn dữ liệu tồn.
        Map<Integer, SkuBulkUpdateRequest.SkuUpdateItem> itemsById = new java.util.LinkedHashMap<>();
        for (SkuBulkUpdateRequest.SkuUpdateItem item : request.getSkus()) {
            if (itemsById.putIfAbsent(item.getSkuId(), item) != null) {
                throw new IllegalArgumentException("SKU ID " + item.getSkuId() + " bị lặp trong danh sách cập nhật.");
            }
        }
        List<Integer> sortedIds = itemsById.keySet().stream().sorted().toList();
        List<ProductSku> skusToUpdate = skuRepository.findByIdsForUpdate(sortedIds);
        if (skusToUpdate.size() != sortedIds.size()) {
            Set<Integer> foundIds = skusToUpdate.stream().map(ProductSku::getSkuId).collect(Collectors.toSet());
            Integer missingId = sortedIds.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new IllegalArgumentException("Không tìm thấy SKU ID: " + missingId);
        }
        for (ProductSku sku : skusToUpdate) {
            ensureActive(sku);
            ensureNoOpenFlashSale(sku);
            SkuBulkUpdateRequest.SkuUpdateItem item = itemsById.get(sku.getSkuId());
            applyPrices(sku, item.getOriginalPrice(), item.getPrice());
        }
        skuRepository.saveAll(skusToUpdate);
    }

    public List<ProductSku> getSkusByProductId(Integer productId) {
        return skuRepository.findByProductProductIdAndIsActiveTrue(productId);
    }

    @Transactional
    public ProductSku updateSingleSku(Integer skuId, BigDecimal originalPrice, BigDecimal price) {
        ProductSku sku = skuRepository.findByIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SKU cần cập nhật."));
        ensureActive(sku);
        ensureNoOpenFlashSale(sku);
        applyPrices(sku, originalPrice, price);
        return skuRepository.save(sku);
    }

    @Transactional
    public void deleteSku(Integer skuId) {
        ProductSku sku = skuRepository.findByIdForUpdate(skuId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SKU cần vô hiệu hóa."));
        if (!Boolean.TRUE.equals(sku.getIsActive())) {
            return;
        }
        sku.setIsActive(false);
        skuRepository.save(sku);
    }

    // Marcus thêm: upload ảnh biến thể một lần rồi gắn cùng URL cho những SKU
    // được Admin chọn (thường là các dung lượng có cùng màu). Ảnh này không được
    // sao chép sang Product_Images vì đó là thư viện ảnh chung của sản phẩm.
    @Transactional(rollbackFor = Exception.class)
    public List<SkuImageUpdateResponse> updateSkuImages(List<Integer> skuIds, MultipartFile file) {
        if (skuIds == null || skuIds.isEmpty() || skuIds.size() > 100) {
            throw new IllegalArgumentException("Vui lòng chọn từ 1 đến 100 SKU để áp dụng ảnh.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh biến thể.");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh biến thể không được vượt quá 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Tệp tải lên phải là hình ảnh.");
        }

        List<Integer> distinctIds = skuIds.stream().distinct().sorted().toList();
        if (distinctIds.size() != skuIds.size()) {
            throw new IllegalArgumentException("Danh sách SKU áp dụng ảnh đang bị trùng.");
        }
        List<ProductSku> skus = skuRepository.findByIdsForUpdate(distinctIds);
        if (skus.size() != distinctIds.size()) {
            throw new IllegalArgumentException("Có SKU không tồn tại hoặc đã bị xóa.");
        }
        Integer productId = skus.get(0).getProduct().getProductId();
        if (skus.stream().anyMatch(sku -> !productId.equals(sku.getProduct().getProductId()))) {
            throw new IllegalArgumentException("Chỉ được áp dụng ảnh cho các SKU của cùng một sản phẩm.");
        }
        skus.forEach(this::ensureActive);

        final String imageUrl;
        try {
            imageUrl = cloudinaryService.uploadImage(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải ảnh biến thể. Vui lòng thử lại.", exception);
        }
        skus.forEach(sku -> sku.setSkuImageUrl(imageUrl));
        return skuRepository.saveAll(skus).stream()
                .map(sku -> new SkuImageUpdateResponse(sku.getSkuId(), sku.getSkuImageUrl()))
                .toList();
    }

    private void ensureActive(ProductSku sku) {
        if (!Boolean.TRUE.equals(sku.getIsActive())) {
            throw new IllegalArgumentException("SKU " + sku.getSkuCode() + " đã ngừng hoạt động.");
        }
    }

    private void applyPrices(ProductSku sku, BigDecimal originalPrice, BigDecimal price) {
        if (originalPrice == null || originalPrice.signum() <= 0 || price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("Giá niêm yết và giá bán phải lớn hơn 0.");
        }
        if (price.compareTo(originalPrice) > 0) {
            throw new IllegalArgumentException("Giá bán không được lớn hơn giá niêm yết.");
        }
        sku.setOriginalPrice(originalPrice);
        sku.setPrice(price);
    }

    private void ensureNoOpenFlashSale(ProductSku sku) {
        if (flashSaleItemRepository.existsOpenFlashSaleForSku(sku.getSkuId(), LocalDateTime.now())) {
            throw new IllegalArgumentException("SKU " + sku.getSkuCode()
                    + " đang thuộc Flash Sale chưa kết thúc. Hãy kết thúc hoặc hủy chương trình trước khi sửa giá thường.");
        }
    }

    private String combinationKey(List<AttributeValue> values) {
        return values.stream()
                .map(AttributeValue::getValueId)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }
}
