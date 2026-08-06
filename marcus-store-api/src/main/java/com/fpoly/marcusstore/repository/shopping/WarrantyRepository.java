package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.WarrantyReturn;
import com.fpoly.marcusstore.entity.shopping.WarrantyReturn.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<WarrantyReturn, Integer> {

    List<WarrantyReturn> findByUserUserIdOrderByCreatedAtDesc(Integer userId);

    List<WarrantyReturn> findByStatusOrderByCreatedAtDesc(WarrantyReturn.WarrantyStatus status);

    @Query("SELECT w FROM WarrantyReturn w ORDER BY CASE WHEN w.status = 'PENDING' THEN 0 ELSE 1 END, w.createdAt DESC")
    List<WarrantyReturn> findAllOrderByPendingFirst();

    @Query("SELECT w FROM WarrantyReturn w WHERE " +
            "(:reason IS NULL OR w.reason = :reason) AND " +
            "(:keyword IS NULL OR LOWER(w.orderItem.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.orderItem.sku.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY CASE WHEN w.status = 'PENDING' THEN 0 ELSE 1 END, w.createdAt DESC")
    List<WarrantyReturn> findAllWithFiltersOrderByPendingFirst(
            @Param("reason") WarrantyReturn.WarrantyReason reason,
            @Param("keyword") String keyword);

    @Query("SELECT w FROM WarrantyReturn w WHERE " +
            "(:status IS NULL OR w.status = :status) AND " +
            "(:reason IS NULL OR w.reason = :reason) AND " +
            "(:keyword IS NULL OR LOWER(w.orderItem.order.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(w.orderItem.sku.product.productName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<WarrantyReturn> searchWarranties(@Param("status") WarrantyStatus status,
                                           @Param("reason") WarrantyReturn.WarrantyReason reason,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    @Query("SELECT COUNT(w) FROM WarrantyReturn w WHERE w.status = :status")
    Long countByStatus(@Param("status") WarrantyReturn.WarrantyStatus status);

    boolean existsByOrderItemOrderItemIdAndUserUserIdAndStatusIn(
            Integer orderItemId,
            Integer userId,
            List<WarrantyReturn.WarrantyStatus> statuses);

    Optional<WarrantyReturn> findByOrderItemOrderItemIdAndUserUserId(Integer orderItemId, Integer userId);
}
