package com.fpoly.marcusstore.repository.shopping;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fpoly.marcusstore.entity.shopping.Wishlist;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

     @Query(value = """
            SELECT DISTINCT w
            FROM Wishlist w
            JOIN FETCH w.product p
            WHERE w.user.userId = :userId
            """,
            countQuery = "SELECT COUNT(DISTINCT w) FROM Wishlist w WHERE w.user.userId = :userId")
    Page<Wishlist> findMyWishlistPage(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT w.wishlistId FROM Wishlist w WHERE w.user.userId = :userId")
    List<Integer> findWishlistIdsByUserId(@Param("userId") Integer userId);

    boolean existsByUserUserIdAndProductProductId(Integer userId, Integer productId);

    long countByUserUserId(Integer userId);

    @Query("SELECT w.product.productId FROM Wishlist w WHERE w.user.userId = :userId")
    List<Integer> findProductIdsByUserId(@Param("userId") Integer userId);

    long deleteByUserUserIdAndProductProductId(Integer userId, Integer productId);
}