package com.fpoly.marcusstore.repository.cms;

import com.fpoly.marcusstore.entity.cms.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostCategoryRepository extends JpaRepository<PostCategory, Integer> {

    // Kiểm tra name đã tồn tại chưa (dùng khi thêm)
    boolean existsByName(String name);

    // Kiểm tra slug đã tồn tại chưa (dùng khi thêm)
    boolean existsBySlug(String slug);

    // Kiểm tra name trùng với category khác (dùng khi sửa)
    boolean existsByNameAndPostCategoryIdNot(String name, Integer postCategoryId);

    // Kiểm tra slug trùng với category khác (dùng khi sửa)
    boolean existsBySlugAndPostCategoryIdNot(String slug, Integer postCategoryId);
}