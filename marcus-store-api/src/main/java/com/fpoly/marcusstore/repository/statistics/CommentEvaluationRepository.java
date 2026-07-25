package com.fpoly.marcusstore.repository.statistics;

import com.fpoly.marcusstore.entity.interaction.CommentEvaluation;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentEvaluationRepository extends JpaRepository<CommentEvaluation, Integer> {

    // Lấy tất cả đánh giá của sản phẩm theo productId, sắp xếp theo createdAt giảm dần
    List<CommentEvaluation> findByProductProductIdOrderByCreatedAtDesc(Integer productId);

    //  Kiểm tra user đã đánh giá sản phẩm chưa
    boolean existsByOrderItemOrderItemId(Integer orderItemId);

    // Lấy đánh giá của user cho sản phẩm
    Optional<CommentEvaluation> findByOrderItemOrderItemId(Integer orderItemId);

    // Lấy tất cả đánh giá của user
    List<CommentEvaluation> findByUserUserId(Integer userId);

@EntityGraph(attributePaths = {
        "user",
        "product",
        "orderItem",
        "images",
        "reply",
        "reply.staff"
})
List<CommentEvaluation> findAllByOrderByCreatedAtDesc();
@EntityGraph(attributePaths = {
        "user",
        "product",
        "orderItem",
        "images",
        "reply",
        "reply.staff"
})
Page<CommentEvaluation> findAllByOrderByCreatedAtDesc(Pageable pageable);
@Query("""
SELECT r
FROM CommentEvaluation r
WHERE
(:keyword IS NULL
OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%',:keyword,'%'))
OR LOWER(r.product.productName) LIKE LOWER(CONCAT('%',:keyword,'%')))
AND (:rating IS NULL OR r.rating=:rating)
AND (
    :replied IS NULL
    OR (:replied=true AND r.reply IS NOT NULL)
    OR (:replied=false AND r.reply IS NULL)
)
""")
Page<CommentEvaluation> search(
        @Param("keyword") String keyword,
        @Param("rating") Integer rating,
        @Param("replied") Boolean replied,
        Pageable pageable);

   @Query("""
SELECT r.rating, COUNT(r)
FROM CommentEvaluation r
WHERE r.product.productId = :productId
GROUP BY r.rating
ORDER BY r.rating DESC
""")
List<Object[]> getRatingStatistics(Integer productId);   

@Query("""
SELECT
c.product.productId,
c.product.productName,
COUNT(c.reviewId)
FROM CommentEvaluation c
GROUP BY
c.product.productId,
c.product.productName
ORDER BY COUNT(c.reviewId) DESC
""")
List<Object[]> getProductsHasReview();
}
