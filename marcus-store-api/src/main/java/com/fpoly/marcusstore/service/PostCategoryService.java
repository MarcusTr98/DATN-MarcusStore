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

    //Chuyển PostCategory entity → PostCategoryResponseDTO
    private PostCategoryResponseDTO toResponse(PostCategory category) {
        return PostCategoryResponseDTO.builder()
                .id(category.getPostCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .status(category.getStatus())
                .build();
    }

    // Lấy tất cả danh mục
    @Transactional(readOnly = true)
    public List<PostCategoryResponseDTO> getAll() {
        return postCategoryRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Lấy chi tiết 1 danh mục theo ID
    @Transactional(readOnly = true)
    public PostCategoryResponseDTO getOne(Integer id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));
        return toResponse(category);
    }

    // Thêm danh mục mới
    public PostCategoryResponseDTO add(PostCategoryRequestDTO req) {
        // Kiểm tra name đã tồn tại chưa
        if (postCategoryRepository.existsByName(req.getName())) {
            throw new RuntimeException("Tên danh mục '" + req.getName() + "' đã tồn tại");
        }

        // Kiểm tra slug đã tồn tại chưa
        if (postCategoryRepository.existsBySlug(req.getSlug())) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        PostCategory category = new PostCategory();
        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setStatus(req.getStatus());

        return toResponse(postCategoryRepository.save(category));
    }

    // Sửa danh mục theo ID
    public PostCategoryResponseDTO update(Integer id, PostCategoryRequestDTO req) {
        // Kiểm tra danh mục tồn tại
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        // Kiểm tra name trùng với category khác
        if (postCategoryRepository.existsByNameAndPostCategoryIdNot(req.getName(), id)) {
            throw new RuntimeException("Tên danh mục '" + req.getName() + "' đã tồn tại");
        }

        // Kiểm tra slug trùng với category khác
        if (postCategoryRepository.existsBySlugAndPostCategoryIdNot(req.getSlug(), id)) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        category.setName(req.getName());
        category.setSlug(req.getSlug());
        category.setStatus(req.getStatus());

        return toResponse(postCategoryRepository.save(category));
    }

    // Xóa mềm: chỉ set status = false, không xóa khỏi DB
    public void remove(Integer id) {
        PostCategory category = postCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));
        category.setStatus(false);
        postCategoryRepository.save(category);
    }
}