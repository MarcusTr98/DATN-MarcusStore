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

    @Autowired
    private ProductSkuRepository skuRepository;

    @Autowired
    private ProductItemService productItemService;

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
                .statusImei(sku.getProduct() != null ? sku.getProduct().getStatusImei() : null)
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
            if (hasImei != null) {
                // Lọc theo product.statusImei = hasImei
                // Trong SQL Server, BIT NULL = 0 nếu filter = false, nên IS NOT NULL là đủ để phân biệt true
                if (hasImei) {
                    predicates.add(cb.isTrue(root.join("product").get("statusImei")));
                } else {
                    // không có IMEI: statusImei = false HOẶC null (chưa set)
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
    public InventorySummaryDTO getInventorySummary() {
        long totalSkus = skuRepository.count();
        List<ProductSku> allSku = skuRepository.findAll();

        long inStock = 0;
        long lowStock = 0;
        long outOfStock = 0;
        int totalUnits = 0;
        BigDecimal totalValue = BigDecimal.ZERO;

        List<LowStockResponseDTO> lowList = new ArrayList<>();

        for (ProductSku sku : allSku) {
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

        int before = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();

        List<String> cleanedImeis = (request.getImeis() == null)
                ? List.of()
                : request.getImeis().stream()
                        .filter(s -> s != null && !s.trim().isEmpty())
                        .map(String::trim)
                        .distinct()
                        .toList();

        if (!cleanedImeis.isEmpty() && cleanedImeis.size() != request.getImportQuantity()) {
            throw new RuntimeException(
                "Số lượng IMEI (" + cleanedImeis.size()
                + ") phải bằng số lượng nhập (" + request.getImportQuantity() + ")");
        }

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
