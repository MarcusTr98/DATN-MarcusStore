package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Integer> {

    @Query("SELECT t FROM OrderTransaction t JOIN FETCH t.order ORDER BY t.createdAt DESC")
    List<OrderTransaction> findAllTransactionsWithOrder();

    Optional<OrderTransaction> findFirstByOrder_OrderIdAndTypeAndStatusOrderByCreatedAtDesc(
            Integer orderId, String type, String status);

    Optional<OrderTransaction> findFirstByOrder_OrderIdAndTypeOrderByCreatedAtDesc(
            Integer orderId, String type);

    boolean existsByOrder_OrderIdAndTypeAndStatus(Integer orderId, String type, String status);

    @Modifying
    @Query(value = """
            INSERT INTO Order_Transactions
                (order_id, amount, type, status, note, created_at, is_reconciled, idempotency_key)
            SELECT
                :orderId, :amount, :type, :status, :note, GETDATE(), 0, :idempotencyKey
            WHERE NOT EXISTS (
                SELECT 1
                FROM Order_Transactions WITH (UPDLOCK, HOLDLOCK)
                WHERE idempotency_key = :idempotencyKey
            )
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("orderId") Integer orderId,
            @Param("amount") java.math.BigDecimal amount,
            @Param("type") String type,
            @Param("status") String status,
            @Param("note") String note,
            @Param("idempotencyKey") String idempotencyKey);
}
