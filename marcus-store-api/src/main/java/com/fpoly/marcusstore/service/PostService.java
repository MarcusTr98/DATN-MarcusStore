package com.fpoly.marcusstore.service;

import com.fpoly.marcusstore.dto.request.PostRequestDTO;
import com.fpoly.marcusstore.dto.response.PostResponseDTO;
import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.entity.cms.Post;
import com.fpoly.marcusstore.entity.cms.PostCategory;
import com.fpoly.marcusstore.repository.cms.PostCategoryRepository;
import com.fpoly.marcusstore.repository.cms.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCategoryRepository postCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    // Chuyển Post entity → PostResponseDTO tránh duplicate code
     
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

        // Gắn thông tin danh mục nếu có
        if (post.getPostCategory() != null) {
            builder.postCategoryId(post.getPostCategory().getPostCategoryId())
                    .postCategoryName(post.getPostCategory().getName())
                    .postCategorySlug(post.getPostCategory().getSlug());
        }

        // Gắn thông tin tác giả nếu có
        if (post.getAuthor() != null) {
            builder.authorId(post.getAuthor().getUserId())
                    .authorName(post.getAuthor().getUsername());
        }

        return builder.build();
    }

    // Lấy tất cả post
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getAll() {
        return postRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết 1 post theo ID
    @Transactional(readOnly = true)
    public PostResponseDTO getOne(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));
        return toResponse(post);
    }

    // Thêm post mới
    public PostResponseDTO add(PostRequestDTO req) {
        // Kiểm tra slug đã tồn tại chưa
        if (postRepository.existsBySlug(req.getSlug())) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        // Kiểm tra category tồn tại
        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        // Kiểm tra author tồn tại
        User author = userRepository.findById(req.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả với id: " + req.getAuthorId()));

        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setSlug(req.getSlug());
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setIsPublished(req.getIsPublished());
        post.setPostCategory(category);
        post.setAuthor(author);

        // Nếu publish thì ghi lại thời gian publish
        if (Boolean.TRUE.equals(req.getIsPublished())) {
            post.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(postRepository.save(post));
    }

    // Sửa post theo ID
    public PostResponseDTO update(Integer id, PostRequestDTO req) {
        // Kiểm tra post tồn tại
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));

        // Kiểm tra slug trùng với post khác
        if (postRepository.existsBySlugAndPostIdNot(req.getSlug(), id)) {
            throw new RuntimeException("Slug '" + req.getSlug() + "' đã tồn tại");
        }

        // Kiểm tra category tồn tại
        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        // Kiểm tra author tồn tại
        User author = userRepository.findById(req.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả với id: " + req.getAuthorId()));

        post.setTitle(req.getTitle());
        post.setSlug(req.getSlug());
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setPostCategory(category);
        post.setAuthor(author);

        // Nếu chuyển sang publish lần đầu thì ghi lại thời gian
        if (Boolean.TRUE.equals(req.getIsPublished()) && !Boolean.TRUE.equals(post.getIsPublished())) {
            post.setPublishedAt(LocalDateTime.now());
        }
        post.setIsPublished(req.getIsPublished());

        return toResponse(postRepository.save(post));
    }

    // Xóa mềm
    public void remove(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));
        post.setIsPublished(false);
        postRepository.save(post);
    }
}