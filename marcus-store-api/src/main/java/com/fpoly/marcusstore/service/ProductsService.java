package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.CreateProduct;
import com.fpoly.marcusstore.dto.request.UpdateProduct;
import com.fpoly.marcusstore.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductsService {

    Page<ProductResponse> findAllProducts(String keyword, String filter, String brand, Pageable pageable);

    ProductResponse createProduct(CreateProduct createProduct);

    Optional<ProductResponse> getProductsById(Integer id);

    ProductResponse updateProduct(Integer id, UpdateProduct updateProduct);

    ProductResponse hiddenProduct(Integer id);

    List<String> getAllDistinctBrands();
}