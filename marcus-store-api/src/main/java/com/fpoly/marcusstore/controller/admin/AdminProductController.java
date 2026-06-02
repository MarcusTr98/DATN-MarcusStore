package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.entity.core.Product;
import com.fpoly.marcusstore.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {
    @Autowired
    ProductsService productsService;

    @GetMapping
    public Page<Map<String, Object>> findAllProducts(Pageable pageable) {
        return productsService.findAllProducts(pageable);
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productsService.createProduct(product);
    }

    @GetMapping("/{id}")
    public Optional<Map<String, Object>> getProductById(@PathVariable Integer id) {
        return productsService.getProductsById(id);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Integer id, @RequestBody Product product) {
        return productsService.updateProduct(id, product);
    }

    @PutMapping("/hidden/{id}")
    public Product hiddenProduct(@PathVariable Integer id) {
        return productsService.hiddenProduct(id);
    }

}
