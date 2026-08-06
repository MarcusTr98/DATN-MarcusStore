package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.WarrantyReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<WarrantyReturn, Integer> {
    
    List<WarrantyReturn> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    
    List<WarrantyReturn> findByOrderItemOrderItemIdOrderByCreatedAtDesc(Integer orderItemId);
    
    List<WarrantyReturn> findByStatusOrderByCreatedAtDesc(WarrantyReturn.WarrantyStatus status);
    
    List<WarrantyReturn> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT w FROM WarrantyReturn w WHERE w.user.userId = :userId AND w.orderItem.orderItemId = :orderItemId AND w.status = :status")
    Optional<WarrantyReturn> findByUserAndOrderItemAndStatus(
            @Param("userId") Integer userId,
            @Param("orderItemId") Integer orderItemId,
            @Param("status") WarrantyReturn.WarrantyStatus status);
    
    @Query("SELECT COUNT(w) FROM WarrantyReturn w WHERE w.status = :status")
    Long countByStatus(@Param("status") WarrantyReturn.WarrantyStatus status);
    
    boolean existsByOrderItemOrderItemIdAndUserUserIdAndStatusIn(
            Integer orderItemId, 
            Integer userId, 
            List<WarrantyReturn.WarrantyStatus> statuses);
    
    Optional<WarrantyReturn> findByOrderItemOrderItemIdAndUserUserId(Integer orderItemId, Integer userId);
}
