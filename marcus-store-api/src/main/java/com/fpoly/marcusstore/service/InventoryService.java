package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.InventoryAdjustRequest;
import com.fpoly.marcusstore.dto.request.StockImportRequest;
import com.fpoly.marcusstore.dto.response.*;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private static final java.util.regex.Pattern IMEI_PATTERN =
            java.util.regex.Pattern.compile("^[0-9]{8,20}$");

    @Autowired
    private ProductSkuRepository skuRepository;

    @Autowired
    private ProductItemService productItemService;

    private boolean isImeiProduct(ProductSku sku) {
        return sku != null
                && sku.getProduct() != null
                && Boolean.TRUE.equals(sku.getProduct().getStatusImei());
    }

    private InventoryResponse toInventoryDTO(ProductSku sku) {
        String productName = "";
        String categoryName = "";
        String brand = "";
        if (sku.getProduct() != null) {
            productName = sku.getProduct().getProductName();
            brand = sku.getProduct().getBrand();
            if (sku.getProduct().getCategory() != null) {
                categoryName = sku.getProduct().getCategory().getCategoryName();
            }
        }

        String stockStatus = "IN_STOCK";
        Integer stock = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
        if (stock == 0) stockStatus = "OUT_OF_STOCK";
        else if (stock <= 5) stockStatus = "LOW_STOCK";

        boolean imei = isImeiProduct(sku);
        return InventoryResponse.builder()
                .skuId(sku.getSkuId())
                .skuCode(sku.getSkuCode())
                .skuImageUrl(sku.getSkuImageUrl())
                .productName(productName)
                .categoryName(categoryName)
                .brand(brand)
                .price(sku.getPrice())
                .stockQuantity(stock)
                .isActive(sku.getIsActive())
                .stockStatus(stockStatus)
                .statusImei(imei)
                .warehouseType(imei ? "IMEI" : "NON_IMEI")
                .build();
    }

    private LowStockResponseDTO buildLowStockDTO(ProductSku sku, Integer qty) {
        String productName = sku.getProduct() != null ? sku.getProduct().getProductName() : "";
        String brand = sku.getProduct() != null ? sku.getProduct().getBrand() : "";
        String status = qty == 0 ? "HẾT HÀNG" : "SẮP HẾT";

        return LowStockResponseDTO.builder()
                .skuCode(sku.getSkuCode())
                .productName(productName)
                .brand(brand)
                .stockQuantity(qty)
                .status(status)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventoryList(
            String keyword, String stockStatus, Boolean hasImei, Pageable pageable) {

        final String statusFilter = stockStatus == null ? null : stockStatus.toUpperCase();
        final Boolean hasImeiFilter = hasImei;

        Specification<ProductSku> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("skuCode")), like),
                        cb.like(cb.lower(root.join("product").get("productName")), like)
                ));
            }
            if (statusFilter != null && !statusFilter.isBlank()) {
                switch (statusFilter) {
                    case "IN_STOCK" -> predicates.add(cb.greaterThan(root.get("stockQuantity"), 5));
                    case "LOW_STOCK" -> predicates.add(cb.and(
                            cb.greaterThan(root.get("stockQuantity"), 0),
                            cb.lessThanOrEqualTo(root.get("stockQuantity"), 5)
                    ));
                    case "OUT_OF_STOCK" -> predicates.add(cb.lessThanOrEqualTo(root.get("stockQuantity"), 0));
                    default -> { /* ignore unknown values */ }
                }
            }
            if (hasImeiFilter != null) {
                if (hasImeiFilter) {
                    predicates.add(cb.isTrue(root.join("product").get("statusImei")));
                } else {
                    predicates.add(cb.or(
                            cb.isFalse(root.join("product").get("statusImei")),
                            cb.isNull(root.join("product").get("statusImei"))
                    ));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<ProductSku> page = skuRepository.findAll(spec, pageable);

        return page.map(this::toInventoryDTO);
    }

    @Transactional(readOnly = true)
    public InventorySummaryDTO getInventorySummary(Boolean hasImei) {
        Specification<ProductSku> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasImei != null) {
                if (hasImei) {
                    predicates.add(cb.isTrue(root.join("product").get("statusImei")));
                } else {
                    predicates.add(cb.or(
                            cb.isFalse(root.join("product").get("statusImei")),
                            cb.isNull(root.join("product").get("statusImei"))
                    ));
                }
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        List<ProductSku> filteredSkus = skuRepository.findAll(spec);
        long totalSkus = filteredSkus.size();

        long inStock = 0;
        long lowStock = 0;
        long outOfStock = 0;
        int totalUnits = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        List<LowStockResponseDTO> lowList = new ArrayList<>();

        for (ProductSku sku : filteredSkus) {
            Integer qty = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
            totalUnits += qty;
            if (sku.getPrice() != null) {
                totalValue = totalValue.add(sku.getPrice().multiply(BigDecimal.valueOf(qty)));
            }

            if (qty == 0) outOfStock++;
            else if (qty <= 5) {
                lowStock++;
                lowList.add(buildLowStockDTO(sku, qty));
            } else {
                inStock++;
            }
        }

        return InventorySummaryDTO.builder()
                .warehouseType(hasImei == null ? "ALL" : (hasImei ? "IMEI" : "NON_IMEI"))
                .totalSkus(totalSkus)
                .totalInStock(inStock)
                .totalOutOfStock(outOfStock)
                .totalLowStock(lowStock)
                .totalStockUnits(totalUnits)
                .totalStockValue(totalValue)
                .lowStockProducts(lowList)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryTransactionResponse importStock(StockImportRequest request) {
        ProductSku sku = skuRepository.findById(request.getSkuId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        boolean isImei = isImeiProduct(sku);

        List<String> cleanedImeis = (request.getImeis() == null)
                ? List.of()
                : request.getImeis().stream()
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (isImei) {
            // Kho IMEI: số IMEI phải bằng đúng số lượng nhập.
            if (cleanedImeis.isEmpty()) {
                throw new RuntimeException(
                        "Sản phẩm có quản lý IMEI; phải nhập danh sách IMEI cho từng đơn vị hàng.");
            }
            if (cleanedImeis.size() != request.getImportQuantity()) {
                throw new RuntimeException(
                        "Số IMEI (" + cleanedImeis.size()
                        + ") phải bằng số lượng nhập (" + request.getImportQuantity() + ")");
            }
            // Validate format từng IMEI.
            for (String code : cleanedImeis) {
                if (!IMEI_PATTERN.matcher(code).matches()) {
                    throw new RuntimeException(
                            "IMEI '" + code + "' không hợp lệ (chỉ chấp nhận chữ số, 8-20 ký tự)");
                }
            }
        } else {
            // Kho không IMEI: tuyệt đối không cho nhập IMEI.
            if (!cleanedImeis.isEmpty()) {
                throw new RuntimeException(
                        "Sản phẩm này không quản lý IMEI; không được nhập danh sách IMEI.");
            }
        }

        int before = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
        int after = before + request.getImportQuantity();
        sku.setStockQuantity(after);
        skuRepository.save(sku);

        if (!cleanedImeis.isEmpty()) {
            productItemService.createBatchForSku(sku.getSkuId(), cleanedImeis);
        }

        return InventoryTransactionResponse.builder()
                .skuId(sku.getSkuId())
                .skuCode(sku.getSkuCode())
                .productName(sku.getProduct() != null ? sku.getProduct().getProductName() : "")
                .transactionType("NHAP")
                .quantityBefore(before)
                .quantityChanged(request.getImportQuantity())
                .quantityAfter(after)
                .referenceType("IMPORT")
                .note(request.getNote())
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryTransactionResponse adjustStock(InventoryAdjustRequest request) {
        ProductSku sku = skuRepository.findById(request.getSkuId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        if (isImeiProduct(sku)) {
            throw new RuntimeException(
                    "Sản phẩm có IMEI không được điều chỉnh số lượng trực tiếp. "
                    + "Hãy dùng chức năng nhập kho IMEI hoặc xử lý IMEI để thay đổi tồn.");
        }

        int before = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
        int after = before + request.getAdjustmentQuantity();

        if (after < 0) {
            throw new RuntimeException("Số lượng tồn kho không thể âm!");
        }

        sku.setStockQuantity(after);
        skuRepository.save(sku);

        return InventoryTransactionResponse.builder()
                .skuId(sku.getSkuId())
                .skuCode(sku.getSkuCode())
                .productName(sku.getProduct() != null ? sku.getProduct().getProductName() : "")
                .transactionType("DIEU_CHINH")
                .quantityBefore(before)
                .quantityChanged(request.getAdjustmentQuantity())
                .quantityAfter(after)
                .referenceType("ADJUSTMENT")
                .note(request.getReason())
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public InventoryResponse getSkuDetail(Integer skuId) {
        ProductSku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU!"));
        return toInventoryDTO(sku);
    }
}
