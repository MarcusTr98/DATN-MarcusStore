package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    // Lấy giỏ hàng của User đang đăng nhập
    Optional<Cart> findByUserUserId(Integer userId);
}