package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    List<OrderStatusHistory> findByOrder_OrderIdOrderByCreatedAtAsc(Integer orderId);
}
