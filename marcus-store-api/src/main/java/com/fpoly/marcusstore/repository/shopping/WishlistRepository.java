package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {
    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserUserIdAndProductProductId(Integer userId, Integer productId);
}