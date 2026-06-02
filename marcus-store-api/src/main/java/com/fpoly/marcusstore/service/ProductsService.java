package com.fpoly.marcusstore.service;


import com.fpoly.marcusstore.entity.core.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface ProductsService {
    Page<Map<String, Object>> findAllProducts(Pageable pageable);

    Product createProduct(Product product);

    Optional<Map<String, Object>> getProductsById(Integer id);

    Product updateProduct (Integer id ,Product product);

    Product hiddenProduct (Integer id);
}
