package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Marcus sửa hỗ trợ module kho: chứng minh tồn khả dụng không bằng tổng IMEI
// trong kho khi vẫn còn đơn giữ hàng chưa gán IMEI.
class InventoryAvailabilityServiceTest {
    private ProductSkuRepository skuRepository;
    private ProductItemRepository itemRepository;
    private InventoryAvailabilityService service;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        skuRepository = mock(ProductSkuRepository.class);
        itemRepository = mock(ProductItemRepository.class);
        service = new InventoryAvailabilityService(skuRepository, itemRepository);

        Product product = new Product();
        product.setStatusImei(true);
        sku = new ProductSku();
        sku.setSkuId(10);
        sku.setSkuCode("PHONE-IMEI");
        sku.setStockQuantity(99);
        sku.setProduct(product);
        when(skuRepository.findByIdsForUpdate(List.of(10))).thenReturn(List.of(sku));
    }

    @Test
    void availableStockSubtractsUnassignedReservations() {
        when(itemRepository.countInStockBySkuId(10)).thenReturn(10L);
        when(itemRepository.countReservedWithoutImeiBySkuId(10)).thenReturn(2L);

        var snapshot = service.synchronizeImeiSku(10);

        assertThat(snapshot.physicalInStock()).isEqualTo(10);
        assertThat(snapshot.reservedWithoutImei()).isEqualTo(2);
        assertThat(snapshot.available()).isEqualTo(8);
        assertThat(sku.getStockQuantity()).isEqualTo(8);
        verify(skuRepository).save(sku);
    }

    @Test
    void availableStockNeverBecomesNegative() {
        when(itemRepository.countInStockBySkuId(10)).thenReturn(1L);
        when(itemRepository.countReservedWithoutImeiBySkuId(10)).thenReturn(3L);

        assertThat(service.synchronizeImeiSku(10).available()).isZero();
        assertThat(sku.getStockQuantity()).isZero();
    }
}
