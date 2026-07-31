package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.PostCategoryRequestDTO;
import com.fpoly.marcusstore.dto.response.PostCategoryResponseDTO;
import com.fpoly.marcusstore.entity.cms.PostCategory;
import com.fpoly.marcusstore.repository.cms.PostCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostCategoryService {

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    private PostCategoryResponseDTO toResponse(PostCategory category) {
        return PostCategoryResponseDTO.builder()
                .id(category.getPostCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .status(category.getStatus())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostCategoryResponseDTO> getAll() {
        return postCategoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostCategoryResponseDTO getOne(Integer id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));
        return toResponse(category);
    }

    @Transactional
    public PostCategoryResponseDTO add(PostCategoryRequestDTO req) {
        if (postCategoryRepository.existsByName(req.getName())) {
            throw new RuntimeException("Tên danh mục '" + req.getName() + "' đã tồn tại");
        }
        if (postCategoryRepository.existsBySlug(req.getSlug())) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        PostCategory category = new PostCategory();
        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setStatus(req.getStatus());

        return toResponse(postCategoryRepository.save(category));
    }

    @Transactional
    public PostCategoryResponseDTO update(Integer id, PostCategoryRequestDTO req) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        if (postCategoryRepository.existsByNameAndPostCategoryIdNot(req.getName(), id)) {
            throw new RuntimeException("Tên danh mục '" + req.getName() + "' đã tồn tại");
        }
        if (postCategoryRepository.existsBySlugAndPostCategoryIdNot(req.getSlug(), id)) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setStatus(req.getStatus());

        return toResponse(postCategoryRepository.save(category));
    }

    // Xoá mềm: chỉ set status = false, không xoá khỏi DB
    @Transactional
    public void remove(Integer id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));
        category.setStatus(false);
        postCategoryRepository.save(category);
    }
}