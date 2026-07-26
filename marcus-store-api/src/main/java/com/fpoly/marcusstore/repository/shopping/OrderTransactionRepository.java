package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderTransactionRepository extends JpaRepository<OrderTransaction, Integer> {
    // Marcus thêm: chỉ lấy ID giao dịch VNPAY còn treo để xử lý theo lô nhỏ.
    @Query("""
            SELECT t.order.orderId
            FROM OrderTransaction t
            WHERE t.type = 'VNPAY_PAYMENT'
              AND t.status = 'PENDING'
              AND t.createdAt <= :cutoff
              AND t.order.paymentMethod = 'VNPAY'
              AND t.order.paymentStatus = 'PENDING'
              AND t.order.orderStatus = 'PENDING'
            ORDER BY t.createdAt
            """)
    List<Integer> findExpiredVnPayOrderIds(
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

    @Query("SELECT t FROM OrderTransaction t JOIN FETCH t.order ORDER BY t.createdAt DESC")
    List<OrderTransaction> findAllTransactionsWithOrder();

    // Marcus thêm: lọc theo khoảng [từ đầu ngày, đầu ngày kế tiếp) để không mất
    // giao dịch cuối ngày và không join Order_Items làm nhân số tiền.
    @Query("""
            SELECT t
            FROM OrderTransaction t
            JOIN FETCH t.order
            WHERE t.createdAt >= :fromDateTime
              AND t.createdAt < :toDateTimeExclusive
            ORDER BY t.createdAt DESC
            """)
    List<OrderTransaction> findTransactionsWithOrderBetween(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

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
