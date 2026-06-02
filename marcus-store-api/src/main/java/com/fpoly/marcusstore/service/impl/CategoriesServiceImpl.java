package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.service.CategoriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CategoriesServiceImpl implements CategoriesService {
    @Autowired
    CategoryRepository categoryRepository;

    @Override
    public Page<Map<String, Object>> findAllCategory (Pageable pageable) {
        Page<Category> category = categoryRepository.findAll(pageable);
        return category.map(data -> {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", data.getCategoryId());
            map.put("categoryName", data.getCategoryName());
            map.put("status", data.getStatus());
            return map;
        });
    }

    @Override
    public Category createCategory (Category category) {
        if(categoryRepository.existsCategoriesByCategoryName((category.getCategoryName()))){
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }
        return categoryRepository.save(category);
    }

    @Override
    public Optional<Map<String,Object>> getCategoryById(Integer id){
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(data -> {
            Map<String, Object> map = new HashMap<>();
            map.put("categoryId", data.getCategoryId());
            map.put("categoryName", data.getCategoryName());
            map.put("status", data.getStatus());
            return map;
        });
    }

    @Override
    public Category updateCategory (Integer id, Category category){
        if(categoryRepository.existsById(id)) {
            category.setCategoryId(id);
            return categoryRepository.save(category);
        }else {
            throw new RuntimeException("Không tìm thấy ID danh mục");
        }
    }

    @Override
    public Category hiddenCategory (Integer id){
        Category category = categoryRepository.findById(id).get();
        category.setStatus(false);
        return categoryRepository.save(category);
    }
}
