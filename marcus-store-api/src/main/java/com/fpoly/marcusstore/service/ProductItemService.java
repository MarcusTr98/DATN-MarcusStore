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
    private static final java.util.Set<Integer> ADMIN_EDITABLE_STATUSES = java.util.Set.of(STATUS_IN_STOCK,
            STATUS_WARRANTY, STATUS_ERROR);

    private static final java.util.regex.Pattern IMEI_PATTERN = java.util.regex.Pattern.compile("^[0-9]{8,20}$");

    @Autowired
    private ProductItemRepository productItemRepo;

    @Autowired
    private ProductSkuRepository skuRepository;

    @Autowired
    private InventoryAvailabilityService inventoryAvailabilityService;

    private String normalizeImei(String raw) {
        if (raw == null)
            return null;
        String v = raw.trim();
        return v.isEmpty() ? null : v;
    }

    private void requireImeiSku(ProductSku sku) {
        if (sku == null) {
            throw new RuntimeException("Không tìm thấy SKU!");
        }
        if (sku.getProduct() == null
                || !Boolean.TRUE.equals(sku.getProduct().getStatusImei())) {
            throw new RuntimeException(
                    "Sản phẩm này không quản lý IMEI; tuyệt đối không được tạo IMEI cho SKU "
                            + sku.getSkuCode());
        }
    }

    private void validateImeiFormat(String imei) {
        if (imei == null || imei.isBlank()) {
            throw new RuntimeException("IMEI không được để trống");
        }
        if (!IMEI_PATTERN.matcher(imei).matches()) {
            throw new RuntimeException(
                    "IMEI '" + imei + "' không hợp lệ (chỉ chấp nhận chữ số, 8-20 ký tự)");
        }
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
        if (sku == null)
            return;
        // Marcus sửa hỗ trợ module kho: không dùng COUNT(IN_STOCK) trực tiếp vì
        // Checkout có thể đã giữ hàng trước khi Admin gán IMEI.
        inventoryAvailabilityService.synchronizeImeiSku(sku.getSkuId());
    }

    public static String toStatusLabel(Integer status) {
        if (status == null)
            return "";
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
        requireImeiSku(sku);

        String imei = normalizeImei(request.getImeiCode());
        validateImeiFormat(imei);

        if (productItemRepo.existsByImeiCode(imei)) {
            throw new RuntimeException("IMEI đã tồn tại trong hệ thống: " + imei);
        }
        ProductItem item = new ProductItem();
        item.setProductSku(sku);
        item.setImeiCode(imei);
        // Marcus sửa luồng kho thành viên: IMEI mới nhập kho luôn IN_STOCK.
        // SOLD chỉ được thiết lập qua nghiệp vụ gán IMEI vào đơn hàng.
        item.setStatus(STATUS_IN_STOCK);
        ProductItem saved = productItemRepo.save(item);
        productItemRepo.flush();
        resyncStockFromImeis(sku);
        return toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductItemResponse update(Integer itemId, ProductItemUpdateRequest request) {
        ProductItem item = productItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy IMEI!"));
        requireImeiSku(item.getProductSku());

        if (request.getImeiCode() != null && !request.getImeiCode().isBlank()) {
            String imei = normalizeImei(request.getImeiCode());
            validateImeiFormat(imei);
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
            // Marcus sửa: API quản trị kho không được tự đánh dấu đã bán hoặc sửa
            // IMEI đang gắn với đơn. Các trạng thái này phải qua Order/Warranty flow.
            if (!ADMIN_EDITABLE_STATUSES.contains(newStatus)) {
                throw new RuntimeException("Không thể đặt thủ công trạng thái IMEI này");
            }
            if (statusChanged && (item.getOrderItem() != null || Integer.valueOf(STATUS_SOLD).equals(oldStatus))) {
                throw new RuntimeException("IMEI đã thuộc đơn hàng; hãy xử lý qua nghiệp vụ đơn hàng hoặc bảo hành");
            }
            String note = request.getNote() == null ? null : request.getNote().trim();
            if (statusChanged && (note == null || note.isEmpty())) {
                throw new RuntimeException("Vui lòng nhập ghi chú khi thay đổi trạng thái IMEI.");
            }
            item.setStatus(newStatus);
            productItemRepo.save(item);
            if (statusChanged) {
                productItemRepo.flush();
                resyncStockFromImeis(item.getProductSku());
            }
        }
        String note = request.getNote() == null ? null : request.getNote().trim();
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
        requireImeiSku(item.getProductSku());
        if (item.getStatus() != null && item.getStatus() != STATUS_IN_STOCK) {
            throw new RuntimeException("Chỉ xóa được IMEI đang ở trạng thái 'Trong kho'");
        }
        ProductSku sku = item.getProductSku();
        productItemRepo.delete(item);
        productItemRepo.flush();
        resyncStockFromImeis(sku);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBatchForSku(Integer skuId, List<String> imeiCodes) {
        if (imeiCodes == null || imeiCodes.isEmpty())
            return;
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        requireImeiSku(sku);

        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (String raw : imeiCodes) {
            String imei = normalizeImei(raw);
            if (imei == null)
                continue;
            validateImeiFormat(imei);
            seen.putIfAbsent(imei, true);
        }
        List<String> uniqueImeis = new ArrayList<>(seen.keySet());
        if (uniqueImeis.isEmpty())
            return;

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
        productItemRepo.flush();
        // Marcus sửa hỗ trợ module kho: nhập lô IMEI cũng phải đồng bộ ngay, trước
        // đây đường này bỏ quên resync nên list ngoài và danh sách IMEI bị lệch.
        resyncStockFromImeis(sku);
    }
}
