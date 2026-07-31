package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.dto.response.OrderResponse;
import com.fpoly.marcusstore.entity.shopping.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
  Optional<Order> findByOrderCode(String orderCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.orderCode = :orderCode")
  Optional<Order> findByOrderCodeForUpdate(@Param("orderCode") String orderCode);

  // Marcus thêm: scheduler khóa đúng một đơn trước khi quyết định hủy thanh toán
  // treo.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
  Optional<Order> findByIdForUpdate(@Param("orderId") Integer orderId);

  // Được thêm từ nhánh GHN Webhook
  Optional<Order> findByTrackingCode(String trackingCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.trackingCode = :trackingCode")
  Optional<Order> findByTrackingCodeForUpdate(@Param("trackingCode") String trackingCode);

  // marcus thêm
  List<Order> findByOrderStatus(String orderStatus);

  List<Order> findByOrderStatusIn(List<String> orderStatuses);

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
        AND (:fromDate IS NULL OR CAST(o.createdAt AS LocalDate) >= :fromDate)
        AND (:toDate   IS NULL OR CAST(o.createdAt AS LocalDate) <= :toDate)
      ORDER BY
        CASE WHEN o.orderStatus IN ('COMPLETED', 'CANCELLED', 'PICKED_UP') THEN 1 ELSE 0 END,
        CASE WHEN o.orderStatus = 'PENDING' THEN 0 ELSE 1 END,
        o.createdAt DESC,
        o.orderId DESC
      """)
  Page<Order> searchOrders(
      @Param("keyword") String keyword,
      @Param("paymentMethod") String paymentMethod,
      @Param("orderStatus") String orderStatus,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate,
      Pageable pageable);

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
      @Param("orderStatus") String orderStatus);

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
      @Param("orderStatus") String orderStatus);

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
      @Param("orderStatus") String orderStatus);

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
      @Param("orderStatus") String orderStatus);

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
      @Param("orderStatus") String orderStatus);

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
      @Param("orderStatus") String orderStatus);

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
          ('PENDING',          1),
          ('CONFIRMED',        2),
          ('PROCESSING',       3),
          ('READY_FOR_PICKUP', 4),
          ('PACKED',           5),
          ('SHIPPING',         6),
          ('DELIVERED',        7),
          ('COMPLETED',        8),
          ('FAILED',           9),
          ('CANCELLED',       10)
      ) AS statuses(status_code, sort_order)
      ORDER BY sort_order
      """, nativeQuery = true)
  List<String> findDistinctOrderStatuses();

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

  // Marcus thêm: lấy theo mốc DELIVERED trong lịch sử, không dùng updated_at vì
  // các tác vụ đối soát khác có thể cập nhật đơn và làm sai thời gian chờ.
  @Query(value = """
      SELECT TOP (100) o.order_id
      FROM Orders o
      WHERE o.order_status = 'DELIVERED'
        AND UPPER(COALESCE(o.fulfillment_method, 'DELIVERY')) <> 'STORE_PICKUP'
        AND EXISTS (
          SELECT 1
          FROM Order_Status_History h
          WHERE h.order_id = o.order_id
            AND h.status = 'DELIVERED'
            AND h.created_at <= :cutoff
        )
      ORDER BY o.order_id
      """, nativeQuery = true)
  List<Integer> findDeliveredOrderIdsEligibleForAutoCompletion(
      @Param("cutoff") java.time.LocalDateTime cutoff);

  @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM Order o " +
      "WHERE o.user.userId = :userId AND o.orderStatus = 'COMPLETED'")
  BigDecimal sumTotalSpentByUserId(@Param("userId") Integer userId);
}
