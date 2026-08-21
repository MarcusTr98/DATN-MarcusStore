package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

  @Query("""
      SELECT assignment FROM OrderAssignment assignment
      JOIN FETCH assignment.staff
      LEFT JOIN FETCH assignment.assignedBy
      WHERE assignment.order.orderId = :orderId AND assignment.isCurrent = true
      """)
  Optional<OrderAssignment> findCurrentByOrderId(@Param("orderId") Integer orderId);

  @Query("""
      SELECT COUNT(assignment) FROM OrderAssignment assignment
      WHERE assignment.staff.userId = :staffId
        AND assignment.isCurrent = true
        AND assignment.order.orderStatus IN :activeStatuses
      """)
  long countCurrentActiveOrders(@Param("staffId") Integer staffId,
      @Param("activeStatuses") Collection<String> activeStatuses);

  @Query("""
      SELECT COUNT(assignment) FROM OrderAssignment assignment
      WHERE assignment.staff.userId = :staffId
        AND assignment.isCurrent = true
        AND assignment.order.orderStatus = 'COMPLETED'
      """)
  long countCurrentCompletedOrders(@Param("staffId") Integer staffId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      SELECT assignment FROM OrderAssignment assignment
      WHERE assignment.order.orderId = :orderId AND assignment.isCurrent = true
      """)
  Optional<OrderAssignment> findCurrentByOrderIdForUpdate(@Param("orderId") Integer orderId);

  boolean existsByOrderOrderIdAndStaffUserIdAndIsCurrentTrue(Integer orderId, Integer staffId);
}
