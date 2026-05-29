package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleSlotRepository extends JpaRepository<FlashSaleSlot, Integer> {
    // Tìm khung giờ đang chạy (Trạng thái = 2)
    @Query("SELECT f FROM FlashSaleSlot f WHERE f.status = 2 AND f.startDate <= :now AND f.endDate >= :now")
    List<FlashSaleSlot> findActiveSlots(LocalDateTime now);
}