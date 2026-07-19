package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.RefundRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

        Optional<RefundRequest> findByIdempotencyKey(String idempotencyKey);

        Optional<RefundRequest> findFirstByOrder_OrderCodeOrderByCreatedAtDesc(String orderCode);

        // Marcus thêm điều kiện userId để client không thể xem refund của đơn người
        // khác.
        Optional<RefundRequest> findFirstByOrder_OrderCodeAndOrder_User_UserIdOrderByCreatedAtDesc(
                        String orderCode, Integer userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT r FROM RefundRequest r WHERE r.refundId = :refundId")
        Optional<RefundRequest> findByIdForUpdate(@Param("refundId") Long refundId);

        @Query(value = """
                        SELECT r FROM RefundRequest r
                        JOIN FETCH r.order o
                        JOIN FETCH r.paymentTransaction pt
                        LEFT JOIN FETCH r.refundTransaction rt
                        LEFT JOIN FETCH r.requestedBy
                        LEFT JOIN FETCH r.approvedBy
                        WHERE (:status IS NULL OR r.status = :status)
                        ORDER BY r.createdAt DESC
                        """, countQuery = """
                        SELECT COUNT(r) FROM RefundRequest r
                        WHERE (:status IS NULL OR r.status = :status)
                        """)
        Page<RefundRequest> findPage(@Param("status") String status, Pageable pageable);

        @Query("""
                        SELECT r.refundId FROM RefundRequest r
                        WHERE r.status = 'RETRY_PENDING'
                          AND r.retryCount < r.maxRetries
                          AND r.nextRetryAt <= :now
                        ORDER BY r.nextRetryAt ASC
                        """)
        List<Long> findRetryableIds(@Param("now") LocalDateTime now, Pageable pageable);
}
