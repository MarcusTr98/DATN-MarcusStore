package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.StockImportRequest;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Marcus sửa hỗ trợ module kho: nhập IMEI không được vừa cộng stock_quantity vừa
// tạo Product_Items vì sẽ đếm hai lần.
class InventoryServiceStockImportTest {
    private ProductSkuRepository skuRepository;
    private ProductItemService productItemService;
    private InventoryService service;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        skuRepository = mock(ProductSkuRepository.class);
        productItemService = mock(ProductItemService.class);
        service = new InventoryService();
        ReflectionTestUtils.setField(service, "skuRepository", skuRepository);
        ReflectionTestUtils.setField(service, "productItemService", productItemService);

        Product product = new Product();
        product.setProductName("Điện thoại test");
        product.setStatusImei(true);
        sku = new ProductSku();
        sku.setSkuId(10);
        sku.setSkuCode("PHONE-IMEI");
        sku.setStockQuantity(5);
        sku.setProduct(product);
        when(skuRepository.findById(10)).thenReturn(Optional.of(sku));
    }

    @Test
    void imeiImportUsesBatchAsSingleStockSource() {
        List<String> imeis = List.of("123456789012341", "123456789012342");
        doAnswer(invocation -> {
            sku.setStockQuantity(7); // kết quả service đồng bộ sau khi thêm 2 IMEI
            return null;
        }).when(productItemService).createBatchForSku(10, imeis);

        var result = service.importStock(StockImportRequest.builder()
                .skuId(10).importQuantity(2).imeis(imeis).note("Nhập test").build());

        assertThat(result.getQuantityBefore()).isEqualTo(5);
        assertThat(result.getQuantityAfter()).isEqualTo(7);
        assertThat(result.getQuantityChanged()).isEqualTo(2);
        verify(productItemService).createBatchForSku(10, imeis);
        verify(skuRepository, never()).save(sku);
    }
}
