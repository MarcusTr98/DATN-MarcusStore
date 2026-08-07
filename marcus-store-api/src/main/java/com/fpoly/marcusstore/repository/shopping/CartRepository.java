package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    // Lấy giỏ hàng của User đang đăng nhập
    Optional<Cart> findByUserUserId(Integer userId);

    // Marcus thêm: tuần tự hóa Checkout của cùng một tài khoản. Sau khi chờ
    // lock, service kiểm tra lại checkoutRequestId trước khi đụng tới kho.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.userId = :userId")
    Optional<Cart> findByUserIdForCheckout(@Param("userId") Integer userId);
}
