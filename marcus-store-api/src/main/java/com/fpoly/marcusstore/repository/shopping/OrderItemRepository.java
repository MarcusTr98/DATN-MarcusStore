package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    @Query("""
            SELECT DISTINCT oi
            FROM OrderItem oi
            LEFT JOIN FETCH oi.productItems
            WHERE oi.order.orderId = :orderId
            """)
    List<OrderItem> findWithProductItemsByOrderId(@Param("orderId") Integer orderId);
}
