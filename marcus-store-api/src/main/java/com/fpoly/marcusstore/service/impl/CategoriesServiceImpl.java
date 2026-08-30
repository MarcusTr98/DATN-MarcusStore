package com.fpoly.marcusstore.service.impl;

import com.fpoly.marcusstore.dto.request.CreateCategory;
import com.fpoly.marcusstore.dto.request.UpdateCategory;
import com.fpoly.marcusstore.dto.response.CategoryResponse;
import com.fpoly.marcusstore.entity.core.Category;
import com.fpoly.marcusstore.repository.core.CategoryRepository;
import com.fpoly.marcusstore.repository.core.CategoryRepository.MainCategoryProjection;
import com.fpoly.marcusstore.service.CategoriesService;
import com.fpoly.marcusstore.service.CloudinaryService;
import com.github.slugify.Slugify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoriesServiceImpl implements CategoriesService {
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CloudinaryService cloudinaryService;

    private CategoryResponse toCateResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .categoryImg(category.getCategoriImg())
                .status(category.getStatus())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getCategoryId() : null)
                .parentName(category.getParent() != null ? category.getParent().getCategoryName() : null)
                .build();
    }

    private CategoryResponse toCateResponse(MainCategoryProjection p) {
        return CategoryResponse.builder()
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .categoryImg(p.getCategoryImg())
                .status(p.getStatus())
                .slug(p.getSlug())
                .parentId(null)
                .parentName(null)
                .build();
    }

    private String extractPublicId(String imageUrl) {
        String[] parts = imageUrl.split("/upload/");
        String afterUpload = parts[1];
        return afterUpload.replaceFirst("v\\d+/", "").replaceAll("\\.[^.]+$", "");
    }

    private String uploadLogoOrThrow(MultipartFile file) {
        try {
            return cloudinaryService.uploadImage(file);
        } catch (IOException e) {
            throw new RuntimeException("Upload logo thất bại");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> findAllCategory(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        return categories.map(this::toCateResponse);
    }

    @Override
    public CategoryResponse createCategory(CreateCategory createCategory, MultipartFile file) {
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

        if (file != null && !file.isEmpty()) {
            category.setCategoriImg(uploadLogoOrThrow(file));
        }

        return toCateResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryById(Integer id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(this::toCateResponse);
    }

    @Override
    public CategoryResponse updateCategory(Integer id, UpdateCategory updateCategory, MultipartFile file) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id ko tồn tại"));

        final Slugify slg = Slugify.builder().build();
        String slug = slg.slugify(updateCategory.getCategoryName());

        if (!category.getCategoryName().equals(updateCategory.getCategoryName())) {
            if (categoryRepository.existsByCategoryNameAndCategoryIdNot(
                    updateCategory.getCategoryName(), id)) {
                throw new RuntimeException("Tên Cate đã tồn tại");
            }
            if (categoryRepository.existsBySlugAndCategoryIdNot(slug, id)) {
                throw new RuntimeException("slug đã tồn tại");
            }
        }

        category.setCategoryName(updateCategory.getCategoryName());
        category.setStatus(updateCategory.getStatus());
        category.setSlug(slug);

        if (updateCategory.getParentId() != null) {
            if (updateCategory.getParentId().equals(id)) {
                throw new RuntimeException("Danh mục không thể là cha của chính nó");
            }
            Category parent = categoryRepository.findById(updateCategory.getParentId())
                    .orElseThrow(() -> new RuntimeException("ParentId ko tồn tại"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        if (file != null && !file.isEmpty()) {
            String oldImg = category.getCategoriImg();
            if (oldImg != null && !oldImg.isBlank()) {
                try {
                    cloudinaryService.deleteImage(extractPublicId(oldImg));
                } catch (IOException e) {
                    throw new RuntimeException("Xóa logo cũ thất bại");
                }
            }
            category.setCategoriImg(uploadLogoOrThrow(file));
        }
        return toCateResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse hiddenCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Id danh mục ko tồn tại"));

        if (!category.getStatus()) {
            throw new RuntimeException("Danh mục đã bị ẩn");
        }
        category.setStatus(false);
        return toCateResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveChildren(Integer parentId) {
        return categoryRepository.findByParent_CategoryIdAndStatusTrue(parentId)
                .stream()
                .map(this::toCateResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveChildrenBySlug(String parentSlug) {
        return categoryRepository.findBySlug(parentSlug)
                .map(parent -> categoryRepository.findByParent_CategoryIdAndStatusTrue(parent.getCategoryId()))
                .map(list -> list.stream().map(this::toCateResponse).toList())
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .filter(Category::getStatus)
                .map(this::toCateResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getMainCategoriesWithProducts() {
        return categoryRepository.findMainCategoriesWithProducts()
                .stream()
                .map(this::toCateResponse)
                .toList();
    }
}