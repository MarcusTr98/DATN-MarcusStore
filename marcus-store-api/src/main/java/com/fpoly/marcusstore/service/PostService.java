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

    // Khởi tạo Slugify 1 lần dùng chung
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

        // Gắn thông tin danh mục nếu có
        if (post.getPostCategory() != null) {
            builder.postCategoryId(post.getPostCategory().getPostCategoryId())
                   .postCategoryName(post.getPostCategory().getName())
                   .postCategorySlug(post.getPostCategory().getSlug());
        }

        // Gắn thông tin tác giả nếu có
        if (post.getAuthor() != null) {
            builder.authorId(post.getAuthor().getUserId())
                   .authorName(post.getAuthor().getFullName());
        }

        return builder.build();
    }

    // Lấy tất cả post — có phân trang
    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getAll(Pageable pageable) {
        return postRepository.findAll(pageable).map(this::toResponse);
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
        // Sinh slug tự động từ title
        String slug = slugify.slugify(req.getTitle());

        // Kiểm tra slug đã tồn tại chưa
        if (postRepository.existsBySlug(slug)) {
            throw new RuntimeException("Slug '" + slug + "' đã tồn tại, vui lòng đổi tiêu đề");
        }

        // Kiểm tra category tồn tại
        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        // Lấy author từ user đang login — không cho Frontend truyền authorId
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

        // Sinh slug tự động từ title mới
        String slug = slugify.slugify(req.getTitle());

        // Kiểm tra slug trùng với post khác
        if (postRepository.existsBySlugAndPostIdNot(slug, id)) {
            throw new RuntimeException("Slug '" + slug + "' đã tồn tại, vui lòng đổi tiêu đề");
        }

        // Kiểm tra category tồn tại
        PostCategory category = postCategoryRepository.findById(req.getPostCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + req.getPostCategoryId()));

        post.setTitle(req.getTitle());
        post.setSlug(slug);
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(req.getExcerpt());
        post.setContent(req.getContent());
        post.setPostCategory(category);

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