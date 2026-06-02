package com.fpoly.marcusstore.controller.admin;

import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.service.CategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    @Autowired
    CategoriesService categoriesService;

    @GetMapping
    public Page<Map<String, Object>> findAllCategory(Pageable pageable) {
        return categoriesService.findAllCategory(pageable);
    }

    @PostMapping
    public Category createCategory(@RequestBody Category category) {
        return categoriesService.createCategory(category);
    }

    @GetMapping("/{id}")
    public Optional<Map<String, Object>> getCategoryById(@PathVariable Integer id) {
        return categoriesService.getCategoryById(id);
    }

    @PutMapping("/{id}")
    public Category updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        return categoriesService.updateCategory(id, category);
    }

    @PutMapping("/hidden/{id}")
    public Category hiddenCategory(@PathVariable Integer id) {
        return categoriesService.hiddenCategory(id);
    }
}
