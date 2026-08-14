package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.UpdateProduct;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.repository.core.ProductSpecValueRepository;
import com.fpoly.marcusstore.service.impl.ProductsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Marcus thêm: đổi danh mục phải dọn thông số cũ nhưng chỉnh sản phẩm trong cùng
// danh mục không được làm mất dữ liệu kỹ thuật.
@ExtendWith(MockitoExtension.class)
class ProductsServiceSpecCleanupTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    CategoryRepository categoryRepository;
    @Mock
    ProductSpecValueRepository productSpecValueRepository;

    @InjectMocks
    ProductsServiceImpl service;

    @Test
    void clearsOldSpecValuesWhenCategoryChanges() {
        Category oldCategory = category(1, "Apple");
        Category newCategory = category(2, "Samsung");
        Product product = product(10, oldCategory);
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(newCategory));
        when(productRepository.save(product)).thenReturn(product);

        service.updateProduct(10, request(2));

        verify(productSpecValueRepository).deleteByProductProductId(10);
    }

    @Test
    void keepsSpecValuesWhenCategoryDoesNotChange() {
        Category category = category(1, "Apple");
        Product product = product(10, category);
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);

        service.updateProduct(10, request(1));

        verify(productSpecValueRepository, never()).deleteByProductProductId(10);
    }

    private UpdateProduct request(int categoryId) {
        return UpdateProduct.builder()
                .productName("iPhone 15")
                .description("Mô tả")
                .brand("Apple")
                .thumbnailUrl("https://cdn.example/iphone.jpg")
                .status(true)
                .categoryId(categoryId)
                .build();
    }

    private Product product(int id, Category category) {
        Product product = new Product();
        product.setProductId(id);
        product.setProductName("iPhone 15");
        product.setCategory(category);
        return product;
    }

    private Category category(int id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName(name);
        return category;
    }
}
