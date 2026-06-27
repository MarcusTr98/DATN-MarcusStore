package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.entity.shopping.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);

    List<Order> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    @Query("""
    SELECT o FROM Order o
    WHERE o.isHidden = false
      AND (:keyword IS NULL
        OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
      AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
      AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
    ORDER BY
      CASE WHEN o.orderStatus = 'PENDING' THEN 0 ELSE 1 END,
      o.createdAt DESC,
      o.orderId DESC
    """)
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus,
            Pageable pageable
    );
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
        """)
    long countOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
          AND o.orderStatus = 'PENDING'
        """)
    long countPendingOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );

    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
          AND o.orderStatus = 'CONFIRMED'
        """)
    long countConfirmedOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
          AND o.orderStatus = 'SHIPPING'
        """)
    long countShippingOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
          AND o.orderStatus = 'COMPLETED'
        """)
    long countCompletedOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );
    @Query("""
        SELECT COUNT(o) FROM Order o
        WHERE o.isHidden = false
          AND (:keyword IS NULL
            OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR o.recipientPhone LIKE CONCAT('%', :keyword, '%'))
          AND (:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod)
          AND (:orderStatus IS NULL OR o.orderStatus = :orderStatus)
          AND o.orderStatus = 'CANCELLED'
        """)
    long countCancelledOrders(
            @Param("keyword") String keyword,
            @Param("paymentMethod") String paymentMethod,
            @Param("orderStatus") String orderStatus
    );
    // lấy tổng số sản phẩm trong 1 đơn hàng
    @Query("""
    SELECT COALESCE(SUM(oi.quantity), 0)
    FROM OrderItem oi
    WHERE oi.order.orderId = :orderId
""")
    Integer countItemsByOrderId(@Param("orderId") Integer orderId);
    @Query("SELECT DISTINCT o.paymentMethod FROM Order o WHERE o.paymentMethod IS NOT NULL")
    List<String> findDistinctPaymentMethods();

    @Query(value = """
            SELECT status_code
            FROM (VALUES
                ('PENDING', 1),
                ('PROCESSING', 2),
                ('CONFIRMED', 3),
                ('SHIPPING', 4),
                ('COMPLETED', 5),
                ('CANCELLED', 6),
                ('FAILED', 7)
            ) AS statuses(status_code, sort_order)
            ORDER BY sort_order
            """, nativeQuery = true)
    List<String> findDistinctOrderStatuses();
//  Left join fetch dùng để lấy kèm dữ liệu quan hệ cùng lúc với Order
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.user
            LEFT JOIN FETCH o.voucher
            LEFT JOIN FETCH o.orderItems oi
            LEFT JOIN FETCH oi.sku sku
            LEFT JOIN FETCH sku.product
            WHERE o.orderCode = :orderCode
            """)
    Optional<Order> findDetailByOrderCode(@Param("orderCode") String orderCode);

    Optional<Order> findByOrderCodeAndUserUserId(String OrderCode, Integer userId);
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
            "WHERE o.user.userId = :userId AND o.orderStatus = 'COMPLETED'")
    BigDecimal sumTotalSpentByUserId(@Param("userId") Integer userId);
}
