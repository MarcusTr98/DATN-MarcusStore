package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.repository.core.ProductRepository;
import com.fpoly.marcusstore.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductsServiceImpl implements ProductsService {
    @Autowired
    ProductRepository productRepository;

    @Override
    public Page<Map<String, Object>> findAllProducts (Pageable pageable) {
        Page<Product> product = productRepository.findAll(pageable);
        return product.map(data -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", data.getProductId());
            map.put("productName", data.getProductName());
            map.put("description", data.getDescription());
            map.put("brand", data.getBrand());
            map.put("thumbnailUrl", data.getThumbnailUrl());
            map.put("status", data.getStatus());
            map.put("categoryId", data.getCategory().getCategoryId());
            map.put("createdAt", data.getCreatedAt());
            return map;
        });
    }

    @Override
    public Product createProduct (Product product) {
        if(productRepository.existsProductByProductName(((product.getProductName())))){
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        return productRepository.save(product);
    }

    @Override
    public Optional<Map<String,Object>> getProductsById(Integer id){
        Optional<Product> product = productRepository.findById(id);
        return product.map(data -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productId", data.getProductId());
            map.put("productName", data.getProductName());
            map.put("description", data.getDescription());
            map.put("brand", data.getBrand());
            map.put("thumbnailUrl", data.getThumbnailUrl());
            map.put("status", data.getStatus());
            map.put("categoryId", data.getCategory().getCategoryId());
            map.put("createdAt", data.getCreatedAt());
            return map;
        });
    }

    @Override
    public Product updateProduct (Integer id, Product product){
        if(productRepository.existsById(id)) {
            product.setProductId(id);
            return productRepository.save(product);
        }else {
            throw new RuntimeException("Không tìm thấy ID danh mục");
        }
    }

    @Override
    public Product hiddenProduct (Integer id){
        Product product = productRepository.findById(id).get();
        product.setStatus(false);
        return productRepository.save(product);
    }
}
