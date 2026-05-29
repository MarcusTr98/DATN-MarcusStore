package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    // Check xem User đã thêm SKU này vào giỏ chưa để tăng Quantity thay vì Insert
    Optional<CartItem> findByCartCartIdAndProductSkuSkuId(Integer cartId, Integer skuId);
}