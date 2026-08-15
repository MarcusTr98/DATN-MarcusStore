package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Lấy các danh mục gốc (không có cha) để làm Menu
    List<Category> findByParentIsNullAndStatusTrue();

    boolean existsByCategoryName(String categoryName);

    boolean existsBySlug(String slug);

    boolean existsByCategoryNameAndCategoryIdNot(String categoryName, Integer categoryId);

    boolean existsBySlugAndCategoryIdNot(String slug, Integer categoryId);

    List<Category> findByParent_CategoryIdAndStatusTrue(Integer parentId);

    // Marcus thêm cho bộ thông số kế thừa: kiểm tra cả danh mục con đang ẩn để
    // không tạo hai trường trùng tên khi danh mục được bật lại.
    List<Category> findByParent_CategoryId(Integer parentId);

    // Marcus thêm: lookup Category theo slug để tra id từ slug trên URL.
    Optional<Category> findBySlug(String slug);

    @Query(value = """
            SELECT c.category_id     AS categoryId,
                   c.category_name   AS categoryName,
                   c.categori_img    AS categoryImg,
                   c.status          AS status,
                   c.slug            AS slug
            FROM Categories c
            WHERE c.parent_id IS NULL
              AND c.status = 1
              AND EXISTS (
                  SELECT 1
                  FROM Products p
                  WHERE p.status = 1
                    AND (
                        p.category_id = c.category_id
                        OR p.category_id IN (
                            SELECT child.category_id
                            FROM Categories child
                            WHERE child.parent_id = c.category_id
                        )
                    )
              )
            ORDER BY c.category_name
            """, nativeQuery = true)
    List<MainCategoryProjection> findMainCategoriesWithProducts();

    interface MainCategoryProjection {
        Integer getCategoryId();

        String getCategoryName();

        String getCategoryImg();

        Boolean getStatus();

        String getSlug();
    }
}
