package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {

    @Query("SELECT p FROM Post p WHERE p.isPublished = true AND (p.publishedAt IS NULL OR p.publishedAt <= :now)")
    Page<Post> findPublishedAndReady(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.slug = :slug AND p.isPublished = true AND (p.publishedAt IS NULL OR p.publishedAt <= :now)")
    Optional<Post> findBySlugAndPublishedReady(@Param("slug") String slug, @Param("now") LocalDateTime now);

    Optional<Post> findBySlugAndIsPublishedTrue(String slug);
    Page<Post> findByIsPublishedTrue(Pageable pageable);

    boolean existsBySlug(String slug);
    boolean existsBySlugAndPostIdNot(String slug, Integer postId);
}