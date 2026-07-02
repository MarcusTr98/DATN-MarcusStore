package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.promotion.FlashSaleSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashSaleSlotRepository extends JpaRepository<FlashSaleSlot, Integer> {
    // Tìm khung giờ đang chạy (Trạng thái = 2)
    @Query("SELECT f FROM FlashSaleSlot f WHERE f.status = 2 AND f.startDate <= :now AND f.endDate >= :now")
    List<FlashSaleSlot> findActiveSlots(LocalDateTime now);


    // Đếm tổng số lượng sản phẩm trong 1 slot
    @Query("SELECT COALESCE(SUM(f.flashSaleQuantity), 0) FROM FlashSaleItem f WHERE f.slot.slotId = :slotId")
    Integer countTotalQuantityBySlotId(@Param("slotId") Integer slotId);
    // Phân trang
    @Query("""
            SELECT f FROM FlashSaleSlot f
            WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR f.status = :status)
            """)
    Page<FlashSaleSlot> searchFlashSaleSlots(
            @Param("keyword") String keyword,
            @Param("status") Short status,
            Pageable pageable
    );
    // Tổng số slot theo filter hiện tại
    @Query("""
            SELECT COUNT(f) FROM FlashSaleSlot f
            WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR f.status = :status)
            """)
    long countFlashSaleSlots(@Param("keyword") String keyword, @Param("status") Short status);
    // Số slot đang chạy
    @Query("""
            SELECT COUNT(f) FROM FlashSaleSlot f
            WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR f.status = :status)
              AND f.status = 2
              AND f.startDate <= :now
              AND f.endDate >= :now
            """)
    long countActiveSlots(@Param("keyword") String keyword,
                          @Param("status") Short status,
                          @Param("now") LocalDateTime now);
    // Số slot sắp diễn ra
    @Query("""
            SELECT COUNT(f) FROM FlashSaleSlot f
            WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR f.status = :status)
              AND f.status = 1
              AND f.startDate > :now
            """)
    long countUpcomingSlots(@Param("keyword") String keyword,
                            @Param("status") Short status,
                            @Param("now") LocalDateTime now);
    // Đếm toàn bộ slot đang chạy
    @Query("""
            SELECT COALESCE(SUM(fi.flashSaleQuantity), 0)
            FROM FlashSaleItem fi
            WHERE fi.slot.status = 2
              AND fi.slot.startDate <= :now
              AND fi.slot.endDate >= :now
            """)
    long sumFlashSaleQuantityInActiveSlots(@Param("now") LocalDateTime now);
}
