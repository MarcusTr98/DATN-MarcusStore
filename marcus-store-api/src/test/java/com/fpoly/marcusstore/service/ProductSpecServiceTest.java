package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.ProductSpecsSaveRequest;
import com.fpoly.marcusstore.dto.response.SpecAttributeResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.entity.core.ProductSpecValue;
import com.fpoly.marcusstore.entity.core.SpecAttribute;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSpecValueRepository;
import com.fpoly.marcusstore.repository.core.SpecAttributeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Marcus thêm: test hồi quy cho module thông số của Đức, tập trung vào xóa giá trị,
// phạm vi danh mục và chuẩn hóa kiểu dữ liệu trước khi ghi database.
@ExtendWith(MockitoExtension.class)
class ProductSpecServiceTest {

    @Mock
    SpecAttributeRepository specAttributeRepository;
    @Mock
    ProductSpecValueRepository specValueRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductSpecService service;

    @Test
    void deletesExistingValueWhenAdminClearsInputInsteadOfSavingNull() {
        Category root = category(1, "Điện thoại", null);
        Category apple = category(2, "Apple", root);
        Product product = product(20, apple);
        SpecAttribute battery = attribute(10, "Dung lượng pin", "number", root);
        ProductSpecValue existing = value(100, product, battery, "5000");

        stubProductContext(product, apple, root, List.of(existing), List.of(battery));

        service.saveSpecValuesForProduct(request(product.getProductId(),
                item(existing.getId(), battery.getSpecAttributeId(), "")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSpecValue>> deleted = ArgumentCaptor.forClass(List.class);
        verify(specValueRepository).deleteAll(deleted.capture());
        assertThat(deleted.getValue()).containsExactly(existing);
        verify(specValueRepository).flush();
        verify(specValueRepository, never()).saveAll(any());
    }

    @Test
    void rejectsAttributeOutsideProductCategoryHierarchy() {
        Category phone = category(1, "Điện thoại", null);
        Category apple = category(2, "Apple", phone);
        Category accessory = category(3, "Phụ kiện", null);
        Product product = product(20, apple);
        SpecAttribute connector = attribute(30, "Chuẩn kết nối", "text", accessory);

        stubProductContext(product, apple, phone, List.of(), List.of(connector));

        assertThatThrownBy(() -> service.saveSpecValuesForProduct(request(product.getProductId(),
                item(null, connector.getSpecAttributeId(), "USB-C"))))
                .hasMessageContaining("không thuộc danh mục");
        verify(specValueRepository, never()).saveAll(any());
    }

    @Test
    void normalizesNumberAndBooleanBeforeSaving() {
        Category phone = category(1, "Điện thoại", null);
        Category apple = category(2, "Apple", phone);
        Product product = product(20, apple);
        SpecAttribute screen = attribute(10, "Kích thước màn hình", "number", phone);
        SpecAttribute esim = attribute(11, "Hỗ trợ eSIM", "boolean", apple);

        stubProductContext(product, apple, phone, List.of(), List.of(screen, esim));

        service.saveSpecValuesForProduct(request(product.getProductId(),
                item(null, screen.getSpecAttributeId(), "6,70"),
                item(null, esim.getSpecAttributeId(), "true")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSpecValue>> saved = ArgumentCaptor.forClass(List.class);
        verify(specValueRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ProductSpecValue::getValueText)
                .containsExactly("6.7", "Có");
    }

    @Test
    void returnsSharedParentAttributesBeforeChildSpecificAttributes() {
        Category phone = category(1, "Điện thoại", null);
        Category apple = category(2, "Apple", phone);
        SpecAttribute appleChip = attribute(20, "Chip Apple", "text", apple);
        appleChip.setDisplayOrder(0);
        SpecAttribute screen = attribute(10, "Công nghệ màn hình", "text", phone);
        screen.setDisplayOrder(10);

        when(categoryRepository.findById(2)).thenReturn(Optional.of(apple));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(phone));
        when(specAttributeRepository.findByCategoryIdsWithCategory(List.of(1, 2)))
                .thenReturn(List.of(appleChip, screen));

        List<SpecAttributeResponse> result = service.getAttributesByCategory(2);

        assertThat(result).extracting(SpecAttributeResponse::getSpecAttributeId)
                .containsExactly(10, 20);
    }

    @Test
    void rejectsValueIdBelongingToAnotherProduct() {
        Category phone = category(1, "Điện thoại", null);
        Product product = product(20, phone);
        SpecAttribute screen = attribute(10, "Màn hình", "text", phone);

        when(productRepository.findById(20)).thenReturn(Optional.of(product));
        when(specValueRepository.findByProductProductId(20)).thenReturn(List.of());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(phone));
        when(specAttributeRepository.findAllById(any())).thenReturn(List.of(screen));

        assertThatThrownBy(() -> service.saveSpecValuesForProduct(request(20,
                item(999, 10, "OLED"))))
                .hasMessageContaining("không thuộc sản phẩm");
    }

    private void stubProductContext(
            Product product,
            Category child,
            Category root,
            List<ProductSpecValue> existing,
            List<SpecAttribute> attributes) {
        when(productRepository.findById(product.getProductId())).thenReturn(Optional.of(product));
        when(specValueRepository.findByProductProductId(product.getProductId())).thenReturn(existing);
        when(categoryRepository.findById(child.getCategoryId())).thenReturn(Optional.of(child));
        if (!child.getCategoryId().equals(root.getCategoryId())) {
            when(categoryRepository.findById(root.getCategoryId())).thenReturn(Optional.of(root));
        }
        when(specAttributeRepository.findAllById(any())).thenReturn(attributes);
    }

    private ProductSpecsSaveRequest request(
            Integer productId,
            ProductSpecsSaveRequest.SpecValueItem... items) {
        return ProductSpecsSaveRequest.builder()
                .productId(productId)
                .specs(List.of(items))
                .build();
    }

    private ProductSpecsSaveRequest.SpecValueItem item(Integer id, Integer attributeId, String value) {
        return ProductSpecsSaveRequest.SpecValueItem.builder()
                .id(id)
                .specAttributeId(attributeId)
                .valueText(value)
                .build();
    }

    private Category category(int id, String name, Category parent) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName(name);
        category.setParent(parent);
        return category;
    }

    private Product product(int id, Category category) {
        Product product = new Product();
        product.setProductId(id);
        product.setCategory(category);
        return product;
    }

    private SpecAttribute attribute(int id, String name, String type, Category category) {
        SpecAttribute attribute = new SpecAttribute();
        attribute.setSpecAttributeId(id);
        attribute.setName(name);
        attribute.setDataType(type);
        attribute.setDisplayOrder(0);
        attribute.setCategory(category);
        return attribute;
    }

    private ProductSpecValue value(
            int id,
            Product product,
            SpecAttribute attribute,
            String valueText) {
        ProductSpecValue value = new ProductSpecValue();
        value.setId(id);
        value.setProduct(product);
        value.setSpecAttribute(attribute);
        value.setValueText(valueText);
        return value;
    }
}
