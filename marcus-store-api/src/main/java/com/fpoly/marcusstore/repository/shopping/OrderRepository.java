package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
}