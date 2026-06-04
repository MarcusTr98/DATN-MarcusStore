package com.fpoly.marcusstore.service;


import com.fpoly.marcusstore.dto.request.CreateProduct;
import com.fpoly.marcusstore.dto.request.UpdateProduct;
import com.fpoly.marcusstore.dto.response.ProductResponse;
import com.fpoly.marcusstore.entity.core.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductsService {
    Page<ProductResponse> findAllProducts(Pageable pageable);

    ProductResponse createProduct(CreateProduct createProduct);

    Optional<ProductResponse> getProductsById(Integer id);

    ProductResponse updateProduct (Integer id ,UpdateProduct updateProduct);

    Product hiddenProduct (Integer id);
}
