package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProductItemUpdateRequest;
import com.fpoly.marcusstore.dto.response.ProductItemResponse;
import com.fpoly.marcusstore.entity.core.ProductItem;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductItemService {

    public static final int STATUS_IN_STOCK = 1;
    public static final int STATUS_SOLD = 2;
    public static final int STATUS_WARRANTY = 3;
    public static final int STATUS_ERROR = 4;

    @Autowired
    private ProductItemRepository productItemRepo;

    @Autowired
    private ProductSkuRepository skuRepository;

    private String normalizeImei(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private ProductItemResponse toResponse(ProductItem item) {
        return ProductItemResponse.builder()
                .itemId(item.getItemId())
                .skuId(item.getProductSku() != null ? item.getProductSku().getSkuId() : null)
                .skuCode(item.getProductSku() != null ? item.getProductSku().getSkuCode() : null)
                .imeiCode(item.getImeiCode())
                .status(item.getStatus())
                .statusLabel(toStatusLabel(item.getStatus()))
                .orderItemId(item.getOrderItem() != null ? item.getOrderItem().getOrderItemId() : null)
                .note(item.getNote())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public void resyncStockFromImeis(ProductSku sku) {
        if (sku == null) return;
        // Đếm trực tiếp từ DB để tránh cache cũ, và load lại ProductSku managed
        long inStockCount = productItemRepo.countInStockBySkuId(sku.getSkuId());
        ProductSku managed = skuRepository.findById(sku.getSkuId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU: " + sku.getSkuId()));
        int current = managed.getStockQuantity() == null ? 0 : managed.getStockQuantity();
        int updated = (int) inStockCount;
        if (updated != current) {
            managed.setStockQuantity(updated);
            skuRepository.save(managed);
        }
    }

    public static String toStatusLabel(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case STATUS_IN_STOCK -> "Trong kho";
            case STATUS_SOLD -> "Đã bán";
            case STATUS_WARRANTY -> "Bảo hành";
            case STATUS_ERROR -> "Lỗi";
            default -> "Không xác định";
        };
    }

    @Transactional(readOnly = true)
    public List<ProductItemResponse> getBySku(Integer skuId) {
        return productItemRepo.findByProductSku_SkuIdOrderByItemIdDesc(skuId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductItemResponse create(Integer skuId, ProductItemUpdateRequest request) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        String imei = normalizeImei(request.getImeiCode());
        if (productItemRepo.existsByImeiCode(imei)) {
            throw new RuntimeException("IMEI đã tồn tại trong hệ thống: " + imei);
        }
        Integer status = request.getStatus() == null ? STATUS_IN_STOCK : request.getStatus();
        ProductItem item = new ProductItem();
        item.setProductSku(sku);
        item.setImeiCode(imei);
        item.setStatus(status);
        ProductItem saved = productItemRepo.save(item);
        resyncStockFromImeis(sku);
        return toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductItemResponse update(Integer itemId, ProductItemUpdateRequest request) {
        ProductItem item = productItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy IMEI!"));
        if (request.getImeiCode() != null && !request.getImeiCode().isBlank()) {
            String imei = normalizeImei(request.getImeiCode());
            if (!imei.equals(item.getImeiCode()) && productItemRepo.existsByImeiCode(imei)) {
                throw new RuntimeException("IMEI đã tồn tại trong hệ thống: " + imei);
            }
            item.setImeiCode(imei);
        }
        boolean statusChanged = false;
        if (request.getStatus() != null) {
            Integer oldStatus = item.getStatus();
            Integer newStatus = request.getStatus();
            if (oldStatus == null || !oldStatus.equals(newStatus)) {
                statusChanged = true;
            }
            item.setStatus(newStatus);
            productItemRepo.save(item);
            if (statusChanged) {
                resyncStockFromImeis(item.getProductSku());
            }
        }
        String note = request.getNote() == null ? null : request.getNote().trim();
        if (statusChanged && (note == null || note.isEmpty())) {
            throw new RuntimeException("Vui lòng nhập ghi chú khi thay đổi trạng thái IMEI.");
        }
        if (note != null && !note.isEmpty()) {
            if (note.length() > 500) {
                throw new RuntimeException("Ghi chú tối đa 500 ký tự.");
            }
            item.setNote(note);
        }
        productItemRepo.save(item);
        return toResponse(item);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer itemId) {
        ProductItem item = productItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy IMEI!"));
        if (item.getStatus() != null && item.getStatus() != STATUS_IN_STOCK) {
            throw new RuntimeException("Chỉ xóa được IMEI đang ở trạng thái 'Trong kho'");
        }
        ProductSku sku = item.getProductSku();
        productItemRepo.delete(item);
        resyncStockFromImeis(sku);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBatchForSku(Integer skuId, List<String> imeiCodes) {
        if (imeiCodes == null || imeiCodes.isEmpty()) return;
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));

        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String raw : imeiCodes) {
            String imei = normalizeImei(raw);
            if (imei == null) continue;
            seen.putIfAbsent(imei, true);
        }
        List<String> uniqueImeis = new ArrayList<>(seen.keySet());
        if (uniqueImeis.isEmpty()) return;

        List<String> existed = productItemRepo.findExistingImeiCodes(uniqueImeis);
        if (!existed.isEmpty()) {
            throw new RuntimeException("IMEI đã tồn tại: " + String.join(", ", existed));
        }

        List<ProductItem> items = new ArrayList<>();
        for (String code : uniqueImeis) {
            ProductItem item = new ProductItem();
            item.setProductSku(sku);
            item.setImeiCode(code);
            item.setStatus(STATUS_IN_STOCK);
            items.add(item);
        }
        productItemRepo.saveAll(items);
    }
}
