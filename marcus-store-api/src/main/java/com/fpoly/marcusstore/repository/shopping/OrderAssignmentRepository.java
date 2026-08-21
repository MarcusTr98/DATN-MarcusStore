package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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

  @Query("""
      SELECT assignment.order.orderStatus FROM OrderAssignment assignment
      WHERE assignment.staff.userId = :staffId
        AND assignment.isCurrent = true
        AND assignment.order.orderStatus IN :activeStatuses
      """)
  List<String> findCurrentActiveStatuses(@Param("staffId") Integer staffId,
      @Param("activeStatuses") Collection<String> activeStatuses);

  @Query("""
      SELECT COUNT(assignment) FROM OrderAssignment assignment
      WHERE assignment.staff.userId = :staffId
        AND assignment.assignmentType = :type
        AND assignment.assignedAt >= :since
      """)
  long countAssignmentsByTypeSince(@Param("staffId") Integer staffId,
      @Param("type") String type, @Param("since") LocalDateTime since);

  @Query("""
      SELECT COUNT(DISTINCT assignment.order.orderId) FROM OrderAssignment assignment
      WHERE assignment.staff.userId = :staffId
        AND assignment.assignedAt >= :since
        AND assignment.order.orderStatus = 'COMPLETED'
      """)
  long countCompletedAssignmentsSince(@Param("staffId") Integer staffId,
      @Param("since") LocalDateTime since);
}
