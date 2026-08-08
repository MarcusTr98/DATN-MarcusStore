package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.SkuBatchCreateRequest;
import com.fpoly.marcusstore.entity.core.Attribute;
import com.fpoly.marcusstore.entity.core.AttributeValue;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.repository.core.AttributeValueRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// Marcus thêm: batch SKU phải chặn trùng tổ hợp trước khi ghi bất kỳ dòng nào.
@ExtendWith(MockitoExtension.class)
class ProductConfigServiceTest {
    @Mock
    ProductSkuRepository skuRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    AttributeValueRepository attributeValueRepository;
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

    private SkuBatchCreateRequest.SkuItem item(String code, List<Integer> ids) {
        SkuBatchCreateRequest.SkuItem item = new SkuBatchCreateRequest.SkuItem();
        item.setSkuCode(code);
        item.setPrice(new BigDecimal("1000000"));
        item.setOriginalPrice(new BigDecimal("1200000"));
        item.setStock(5);
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
}
