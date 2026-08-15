package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Marcus sửa hỗ trợ module kho: một nguồn duy nhất đồng bộ tồn khả dụng của SKU
 * quản lý IMEI.
 *
 * Tồn khả dụng = IMEI IN_STOCK - lượng đơn đang giữ nhưng chưa gán IMEI.
 */
@Service
@RequiredArgsConstructor
public class InventoryAvailabilityService {

    private final ProductSkuRepository skuRepository;
    private final ProductItemRepository productItemRepository;

    @Transactional
    public StockSnapshot synchronizeImeiSku(Integer skuId) {
        ProductSku sku = skuRepository.findByIdsForUpdate(List.of(skuId)).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy SKU: " + skuId));

        if (sku.getProduct() == null || !Boolean.TRUE.equals(sku.getProduct().getStatusImei())) {
            throw new IllegalArgumentException("Chỉ đồng bộ theo IMEI cho SKU có quản lý IMEI");
        }

        long physicalInStock = productItemRepository.countInStockBySkuId(skuId);
        long reservedWithoutImei = productItemRepository.countReservedWithoutImeiBySkuId(skuId);
        int available = Math.toIntExact(Math.max(0L, physicalInStock - reservedWithoutImei));

        int previous = sku.getStockQuantity() == null ? 0 : sku.getStockQuantity();
        if (previous != available) {
            sku.setStockQuantity(available);
            skuRepository.save(sku);
        }
        return new StockSnapshot(previous, physicalInStock, reservedWithoutImei, available);
    }

    public record StockSnapshot(
            int previousAvailable,
            long physicalInStock,
            long reservedWithoutImei,
            int available) {
    }
}
