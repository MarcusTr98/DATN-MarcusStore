package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
     Optional<Post> findBySlugAndIsPublishedTrue(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndPostIdNot(String slug, Integer postId);
    Page<Post> findByIsPublishedTrue(Pageable pageable);
}