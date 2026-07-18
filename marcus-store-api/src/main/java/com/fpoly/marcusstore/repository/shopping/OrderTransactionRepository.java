package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Integer> {

    @Query("SELECT t FROM OrderTransaction t JOIN FETCH t.order ORDER BY t.createdAt DESC")
    List<OrderTransaction> findAllTransactionsWithOrder();

    Optional<OrderTransaction> findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
            Integer orderId, String type, String status);

    boolean existsByOrder_OrderIdAndTypeAndStatus(Integer orderId, String type, String status);
}
