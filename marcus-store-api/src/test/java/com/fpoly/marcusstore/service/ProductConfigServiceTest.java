package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.dto.request.SkuBulkUpdateRequest;
import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import com.fpoly.marcusstore.repository.promotion.FlashSaleItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

// Marcus thêm: batch SKU phải chặn trùng tổ hợp trước khi ghi bất kỳ dòng nào.
@ExtendWith(MockitoExtension.class)
class ProductConfigServiceTest {
    @Mock
    ProductSkuRepository skuRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    AttributeValueRepository attributeValueRepository;
    @Mock
    FlashSaleItemRepository flashSaleItemRepository;
    @InjectMocks
    ProductConfigService service;

    @Test
    void rejectsDuplicatedVariantCombinationInsideBatch() {
        Product product = new Product();
        product.setProductId(1);
        Attribute color = attribute(1);
        Attribute capacity = attribute(2);
        AttributeValue black = value(10, color);
        AttributeValue storage = value(20, capacity);

        when(productRepository.findByIdForSkuGeneration(1)).thenReturn(Optional.of(product));
        when(skuRepository.findByProductProductId(1)).thenReturn(List.of());
        when(skuRepository.existsBySkuCodeIgnoreCase(anyString())).thenReturn(false);
        when(attributeValueRepository.findAllById(anyList())).thenReturn(List.of(black, storage));

        SkuBatchCreateRequest request = new SkuBatchCreateRequest();
        request.setProductId(1);
        request.setSkus(List.of(item("phone-black-128", List.of(10, 20)),
                item("PHONE-BLACK-128-B", List.of(20, 10))));

        assertThatThrownBy(() -> service.batchCreateSkus(request))
                .hasMessageContaining("Tổ hợp biến thể")
                .hasMessageContaining("bị trùng");
        verify(skuRepository, never()).saveAll(anyList());
    }

    @Test
    void createsNewSkuWithZeroStockSoInventoryOwnsStockChanges() {
        Product product = new Product();
        product.setProductId(1);
        Attribute color = attribute(1);
        AttributeValue black = value(10, color);
        when(productRepository.findByIdForSkuGeneration(1)).thenReturn(Optional.of(product));
        when(skuRepository.findByProductProductId(1)).thenReturn(List.of());
        when(skuRepository.existsBySkuCodeIgnoreCase(anyString())).thenReturn(false);
        when(attributeValueRepository.findAllById(anyList())).thenReturn(List.of(black));

        SkuBatchCreateRequest request = new SkuBatchCreateRequest();
        request.setProductId(1);
        request.setSkus(List.of(item("PHONE-BLACK", List.of(10))));
        service.batchCreateSkus(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSku>> captor = ArgumentCaptor.forClass(List.class);
        verify(skuRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(ProductSku::getStockQuantity)
                .isEqualTo(0);
    }

    @Test
    void updatesSingleSkuPricesWithoutOverwritingInventory() {
        ProductSku sku = sku(7, "PHONE-128-BLACK", 9);
        when(skuRepository.findByIdForUpdate(7)).thenReturn(Optional.of(sku));
        when(skuRepository.save(sku)).thenReturn(sku);

        ProductSku updated = service.updateSingleSku(
                7, new BigDecimal("12000000"), new BigDecimal("10990000"));

        assertThat(updated.getOriginalPrice()).isEqualByComparingTo("12000000");
        assertThat(updated.getPrice()).isEqualByComparingTo("10990000");
        assertThat(updated.getStockQuantity()).isEqualTo(9);
    }

    @Test
    void rejectsSellingPriceGreaterThanListedPrice() {
        ProductSku sku = sku(7, "PHONE-128-BLACK", 9);
        when(skuRepository.findByIdForUpdate(7)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> service.updateSingleSku(
                7, new BigDecimal("10000000"), new BigDecimal("11000000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được lớn hơn");
        verify(skuRepository, never()).save(any(ProductSku.class));
    }

    @Test
    void rejectsPriceUpdateWhileSkuBelongsToOpenFlashSale() {
        ProductSku sku = sku(7, "PHONE-128-BLACK", 9);
        when(skuRepository.findByIdForUpdate(7)).thenReturn(Optional.of(sku));
        when(flashSaleItemRepository.existsOpenFlashSaleForSku(eq(7), any())).thenReturn(true);

        assertThatThrownBy(() -> service.updateSingleSku(
                7, new BigDecimal("12000000"), new BigDecimal("10990000")))
                .hasMessageContaining("Flash Sale chưa kết thúc");
        verify(skuRepository, never()).save(any(ProductSku.class));
    }

    @Test
    void rejectsDuplicatedSkuIdsInBulkUpdate() {
        SkuBulkUpdateRequest.SkuUpdateItem first = bulkItem(7, "12000000", "11000000");
        SkuBulkUpdateRequest.SkuUpdateItem duplicate = bulkItem(7, "13000000", "11500000");
        SkuBulkUpdateRequest request = new SkuBulkUpdateRequest();
        request.setSkus(List.of(first, duplicate));

        assertThatThrownBy(() -> service.bulkUpdateSkus(request))
                .hasMessageContaining("bị lặp");
        verify(skuRepository, never()).findByIdsForUpdate(anyList());
    }

    private SkuBatchCreateRequest.SkuItem item(String code, List<Integer> ids) {
        SkuBatchCreateRequest.SkuItem item = new SkuBatchCreateRequest.SkuItem();
        item.setSkuCode(code);
        item.setPrice(new BigDecimal("1000000"));
        item.setOriginalPrice(new BigDecimal("1200000"));
        item.setValueIds(ids);
        return item;
    }

    private Attribute attribute(int id) {
        Attribute attribute = new Attribute();
        attribute.setAttributeId(id);
        return attribute;
    }

    private AttributeValue value(int id, Attribute attribute) {
        AttributeValue value = new AttributeValue();
        value.setValueId(id);
        value.setAttribute(attribute);
        return value;
    }

    private ProductSku sku(int id, String code, int stock) {
        ProductSku sku = new ProductSku();
        sku.setSkuId(id);
        sku.setSkuCode(code);
        sku.setStockQuantity(stock);
        sku.setIsActive(true);
        sku.setPrice(new BigDecimal("10000000"));
        sku.setOriginalPrice(new BigDecimal("11000000"));
        return sku;
    }

    private SkuBulkUpdateRequest.SkuUpdateItem bulkItem(int id, String originalPrice, String price) {
        SkuBulkUpdateRequest.SkuUpdateItem item = new SkuBulkUpdateRequest.SkuUpdateItem();
        item.setSkuId(id);
        item.setOriginalPrice(new BigDecimal(originalPrice));
        item.setPrice(new BigDecimal(price));
        return item;
    }
}
