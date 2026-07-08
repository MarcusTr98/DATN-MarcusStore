package com.fpoly.marcusstore.service;
import com.fpoly.marcusstore.dto.request.PostRequestDTO;
import com.fpoly.marcusstore.dto.response.PostResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.cms.Post;
import com.fpoly.marcusstore.entity.cms.PostCategory;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import com.fpoly.marcusstore.repository.cms.PostCategoryRepository;
import com.fpoly.marcusstore.repository.cms.PostRepository;
import com.fpoly.marcusstore.security.SecurityUtils;
import com.github.slugify.Slugify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    private final Slugify slugify = Slugify.builder().build();

    private PostResponseDTO toResponse(Post post) {
        PostResponseDTO.PostResponseDTOBuilder builder = PostResponseDTO.builder()
                .id(post.getPostId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .thumbnailUrl(post.getThumbnailUrl())
                .excerpt(post.getExcerpt())
                .content(post.getContent())
                .isPublished(post.getIsPublished())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt());

        if (post.getPostCategory() != null) {
            builder.postCategoryId(post.getPostCategory().getPostCategoryId())
                   .postCategoryName(post.getPostCategory().getName())
                   .postCategorySlug(post.getPostCategory().getSlug());
        }

        if (post.getAuthor() != null) {
            builder.authorId(post.getAuthor().getUserId())
                   .authorName(post.getAuthor().getFullName());
        }

        return builder.build();
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getAll(Pageable pageable) {
        return postRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponseDTO getOne(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));
        return toResponse(post);
    }
    
    @Transactional(readOnly = true)
    public boolean checkSlugExists(String slug, Integer excludeId) {
        if (slug == null || slug.isBlank()) return false;
        return (excludeId != null)
                ? postRepository.existsBySlugAndPostIdNot(slug, excludeId)
                : postRepository.existsBySlug(slug);
    }
    @Transactional
    public PostResponseDTO add(PostRequestDTO req) {
        String slug = slugify.slugify(req.getTitle());

        if (postRepository.existsBySlug(slug)) {
            throw new RuntimeException("Slug '" + slug + "' đã tồn tại, vui lòng đổi tiêu đề");
        }

        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả với id: " + currentUserId));

        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setSlug(slug);
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setIsPublished(req.getIsPublished());
        post.setPostCategory(category);
        post.setAuthor(author);

        if (Boolean.TRUE.equals(req.getIsPublished())) {
            post.setPublishedAt(req.getPublishedAt() != null ? req.getPublishedAt() : LocalDateTime.now());
        }

        return toResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponseDTO update(Integer id, PostRequestDTO req) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));

        String slug = slugify.slugify(req.getTitle());

        if (postRepository.existsBySlugAndPostIdNot(slug, id)) {
            throw new RuntimeException("Slug '" + slug + "' đã tồn tại, vui lòng đổi tiêu đề");
        }

        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        post.setTitle(req.getTitle());
        post.setSlug(slug);
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setPostCategory(category);

        if (Boolean.TRUE.equals(req.getIsPublished())) {
            if (req.getPublishedAt() != null) {
                post.setPublishedAt(req.getPublishedAt());
            } else if (!Boolean.TRUE.equals(post.getIsPublished())) {
                post.setPublishedAt(LocalDateTime.now());
            }
        }
        post.setIsPublished(req.getIsPublished());

        return toResponse(postRepository.save(post));
    }
    @Transactional
    public void remove(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));
        post.setIsPublished(false);
        postRepository.save(post);
    }
    @Transactional(readOnly = true)
public Page<PostResponseDTO> getPublished(Pageable pageable) {
    return postRepository.findByIsPublishedTrue(pageable).map(this::toResponse);
}

@Transactional(readOnly = true)
public PostResponseDTO getPublishedBySlug(String slug) {
    Post post = postRepository.findBySlugAndIsPublishedTrue(slug)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
    return toResponse(post);
}
}
