package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateProduct;
import com.fpoly.marcusstore.dto.request.UpdateProduct;
import com.fpoly.marcusstore.dto.response.ProductResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.service.ProductsService;
import com.github.slugify.Slugify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ProductsServiceImpl implements ProductsService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository cateRepo;

    // Marcus sửa để làm màn generete SKU admin
    private ProductResponse toProductResponse(Product product) {
        String categoryName = "Chưa phân loại";
        if (product.getCategory() != null) {
            categoryName = product.getCategory().getCategoryName();
        }
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .slug(product.getSlug())
                .status(product.getStatus())
                .thumbnailUrl(product.getThumbnailUrl())
                .createdAt(product.getCreatedAt())
                .categoryName(categoryName)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAllProducts(String keyword, String filter, Pageable pageable) {
        Page<Product> product = productRepository.findProductsWithFilter(keyword, filter, pageable);
        return product.map(this::toProductResponse);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProduct createProduct) {

        if (productRepository.existsByProductName(createProduct.getProductName())) {
            throw new RuntimeException("Tên sản phẩm đã tồn tại");
        }

        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(createProduct.getProductName());

        if (productRepository.existsBySlug(slug)) {
            throw new RuntimeException("Slug đã tồn tại");
        }

        Category category = cateRepo.findById(createProduct.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category"));

        Product product = new Product();
        product.setProductName(createProduct.getProductName());
        product.setDescription(createProduct.getDescription());
        product.setBrand(createProduct.getBrand());
        product.setThumbnailUrl(createProduct.getThumbnailUrl());
        product.setSlug(slug);
        product.setStatus(true);
        product.setCategory(category);

        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductsById(Integer id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(this::toProductResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer id, UpdateProduct updateProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ID sản phẩm"));

        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(updateProduct.getProductName());

        if (!product.getProductName().equals(updateProduct.getProductName())) {
            if (productRepository.existsByProductNameAndProductIdNot(updateProduct.getProductName(), id)) {
                throw new RuntimeException("Tên sản phẩm đã tồn tại");
            }
            if (productRepository.existsBySlug(slug)) {
                throw new RuntimeException("Slug đã tồn tại");
            }
        }

        Category category = cateRepo.findById(updateProduct.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category"));

        product.setProductName(updateProduct.getProductName());
        product.setDescription(updateProduct.getDescription());
        product.setBrand(updateProduct.getBrand());
        product.setThumbnailUrl(updateProduct.getThumbnailUrl());
        product.setSlug(slug);
        product.setStatus(updateProduct.getStatus());
        product.setCategory(category);

        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public Product hiddenProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ID sản phẩm không tồn tại"));

        if (!product.getStatus()) {
            throw new RuntimeException("Sản phẩm đã bị ẩn");
        }
        product.setStatus(false);
        return productRepository.save(product);
    }
}