package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.service.CategoriesService;
import com.github.slugify.Slugify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CategoriesServiceImpl implements CategoriesService {
    @Autowired
    CategoryRepository categoryRepository;

    private CategoryResponse toCateResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .status(category.getStatus())
                .slug(category.getSlug())
                .build();
    }

    @Override
    public Page<CategoryResponse> findAllCategory(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.map(this::toCateResponse);
    }

    @Override
    public CategoryResponse createCategory(CreateCategory createCategory) {
        if (categoryRepository.existsByCategoryName((createCategory.getCategoryName()))) {
            throw new RuntimeException("Tên danh mục đã tồn tại");
        }

        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(createCategory.getCategoryName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new RuntimeException("slug đã tồn tại");
        }

        Category category = new Category();
        category.setCategoryName(createCategory.getCategoryName());
        category.setSlug(slug);
        category.setStatus(true);
        if (createCategory.getParentId() != null) {
            Category parent = categoryRepository.findById(createCategory.getParentId())
                    .orElseThrow(() -> new RuntimeException("ParentId ko tồn tại"));
            category.setParent(parent);
        }
        return toCateResponse(categoryRepository.save(category));
    }

    @Override
    public Optional<CategoryResponse> getCategoryById(Integer id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(this::toCateResponse);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, UpdateCategory updateCategory) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id ko tồn tại"));

        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(updateCategory.getCategoryName());

        if (!category.getCategoryName().equals(updateCategory.getCategoryName())) {
            if (categoryRepository.existsByCategoryNameAndCategoryId(
                    updateCategory.getCategoryName(), id)) {
                throw new RuntimeException("Tên Cate đã tồn tại");
            }
            if (categoryRepository.existsBySlug(slug)) {
                throw new RuntimeException("slug đã tồn tại");
            }
        }

        category.setCategoryName(updateCategory.getCategoryName());
        category.setStatus(updateCategory.getStatus());
        category.setSlug(slug);

        if (updateCategory.getParentId() != null){
            if (updateCategory.getParentId().equals(id)) {
            throw new RuntimeException("Danh mục không thể là cha của chính nó");
        }
            Category parent = categoryRepository.findById(updateCategory.getParentId())
                    .orElseThrow(() -> new RuntimeException("ParentId ko tồn tại"));
            category.setParent(parent);
        } else if (updateCategory.getParentId() == null) {
            category.setParent(null);
        }
        return toCateResponse(categoryRepository.save(category));
    }

    @Override
    public Category hiddenCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id danh mục ko tồn tại"));

        if (!category.getStatus()) {
            throw new RuntimeException("Danh mục đã bị ẩn");
        }
        category.setStatus(false);
        return categoryRepository.save(category);
    }
}
