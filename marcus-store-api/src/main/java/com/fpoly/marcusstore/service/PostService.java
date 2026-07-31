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
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PostService {

    @Autowired private PostRepository postRepository;
    @Autowired private PostCategoryRepository postCategoryRepository;
    @Autowired private UserRepository userRepository;

    private final Slugify slugify = Slugify.builder().build();
    private static final Safelist CONTENT_SAFELIST = Safelist.relaxed()
            .addTags("span")
            .addAttributes("a", "href", "data-product-link")
            .addAttributes("span", "data-hot-badge", "style", "class")
            .addProtocols("a", "href", "#", "http", "https", "/");
    private String sanitizeContent(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, CONTENT_SAFELIST);
    }

    /** Strip HTML lấy plain text, dùng cho auto-excerpt */
    private String stripHtml(String html) {
        if (html == null) return "";
        return Jsoup.parse(html).text();
    }

    private String resolveExcerpt(String excerpt, String content) {
        if (excerpt != null && !excerpt.isBlank()) return excerpt.trim();
        String plain = stripHtml(content).replaceAll("\\s+", " ").trim();
        return plain.length() > 150 ? plain.substring(0, 150) + "…" : plain;
    }

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

        String safeContent = sanitizeContent(req.getContent());

        String excerpt = resolveExcerpt(req.getExcerpt(), safeContent);

        Post post = new Post();
        post.setTitle(req.getTitle());
        post.setSlug(slug);
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(excerpt);
        post.setContent(safeContent);
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

        String safeContent = sanitizeContent(req.getContent());

        String excerpt = resolveExcerpt(req.getExcerpt(), safeContent);

        post.setTitle(req.getTitle());
        post.setSlug(slug);
        post.setThumbnailUrl(req.getThumbnailUrl());
        post.setExcerpt(excerpt);
        post.setContent(safeContent);
        post.setPostCategory(category);

        if (Boolean.TRUE.equals(req.getIsPublished())) {
            if (req.getPublishedAt() != null) {
                post.setPublishedAt(req.getPublishedAt());
            } else if (!Boolean.TRUE.equals(post.getIsPublished())) {
                // Draft → Published lần đầu, không có publishedAt → dùng now()
                post.setPublishedAt(LocalDateTime.now());
            }
            // Đang published + không đổi publishedAt → giữ nguyên timestamp cũ
        } else {
            // Published → Draft: reset publishedAt về null
            post.setPublishedAt(null);
        }
        post.setIsPublished(req.getIsPublished());

        return toResponse(postRepository.save(post));
    }

    @Transactional
    public void unpublish(Integer id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với id: " + id));
        post.setIsPublished(false);
        post.setPublishedAt(null);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDTO> getPublished(Pageable pageable) {
        return postRepository.findPublishedAndReady(LocalDateTime.now(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponseDTO getPublishedBySlug(String slug) {
        Post post = postRepository.findBySlugAndPublishedReady(slug, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        return toResponse(post);
    }
}