package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    // Dùng slug để SEO trên URL (VD: marcus.com/blog/iphone-15-ra-mat)
    Optional<Post> findBySlugAndIsPublishedTrue(String slug);
}