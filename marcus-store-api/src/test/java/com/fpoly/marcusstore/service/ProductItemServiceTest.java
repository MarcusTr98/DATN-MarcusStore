package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProductItemUpdateRequest;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductItem;
import com.fpoly.marcusstore.entity.core.ProductSku;
import com.fpoly.marcusstore.entity.shopping.OrderItem;
import com.fpoly.marcusstore.repository.core.ProductItemRepository;
import com.fpoly.marcusstore.repository.core.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Marcus thêm test hồi quy cho ranh giới quyền giữa màn quản trị kho và nghiệp vụ đơn hàng.
class ProductItemServiceTest {
    private ProductItemRepository itemRepository;
    private ProductSkuRepository skuRepository;
    private ProductItemService service;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ProductItemRepository.class);
        skuRepository = mock(ProductSkuRepository.class);
        service = new ProductItemService();
        ReflectionTestUtils.setField(service, "productItemRepo", itemRepository);
        ReflectionTestUtils.setField(service, "skuRepository", skuRepository);

        Product product = new Product();
        product.setStatusImei(true);
        sku = new ProductSku();
        sku.setSkuId(10);
        sku.setSkuCode("PHONE-IMEI");
        sku.setStockQuantity(0);
        sku.setProduct(product);
        when(skuRepository.findById(10)).thenReturn(Optional.of(sku));
        when(itemRepository.save(any(ProductItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createAlwaysStartsInStockEvenWhenClientSendsSold() {
        ProductItemUpdateRequest request = ProductItemUpdateRequest.builder()
                .imeiCode("123456789012345")
                .status(ProductItemService.STATUS_SOLD)
                .build();

        var response = service.create(10, request);

        assertThat(response.getStatus()).isEqualTo(ProductItemService.STATUS_IN_STOCK);
    }

    @Test
    void adminCannotChangeStatusOfImeiAlreadyAssignedToOrder() {
        ProductItem item = new ProductItem();
        item.setItemId(1);
        item.setImeiCode("123456789012345");
        item.setStatus(ProductItemService.STATUS_SOLD);
        item.setProductSku(sku);
        item.setOrderItem(new OrderItem());
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        ProductItemUpdateRequest request = ProductItemUpdateRequest.builder()
                .imeiCode(item.getImeiCode())
                .status(ProductItemService.STATUS_IN_STOCK)
                .note("Đưa lại kho thủ công")
                .build();

        assertThatThrownBy(() -> service.update(1, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nghiệp vụ đơn hàng");
        verify(itemRepository, never()).save(item);
    }
}
