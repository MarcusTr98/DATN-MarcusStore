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
    // Phân trang - sắp xếp theo thứ tự ưu tiên:
    //   ACTIVE (2) → SCHEDULED (1) → ENDED (3) → CANCELLED (4)
    // Trong cùng nhóm: slot ACTIVE/SCHEDULED/ENDED sắp theo startDate ASC,
    // CANCELLED sắp theo updatedAt DESC (slot hủy sau cùng nằm cuối cùng).
    @Query(value = """
        SELECT f FROM FlashSaleSlot f
        WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR f.status = :status)
          AND (:now IS NULL OR :now = :now)
        ORDER BY
          CASE f.status
            WHEN 2 THEN 1
            WHEN 1 THEN 2
            WHEN 3 THEN 3
            WHEN 4 THEN 4
            ELSE 5
          END,
          CASE WHEN f.status = 4 THEN f.updatedAt ELSE f.startDate END ASC,
          f.slotId ASC
        """,
            countQuery = """
        SELECT COUNT(f) FROM FlashSaleSlot f
        WHERE (:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR f.status = :status)
        """)
    Page<FlashSaleSlot> searchFlashSaleSlots(
            @Param("keyword") String keyword,
            @Param("status") Short status,
            @Param("now") LocalDateTime now,
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

    // Scheduled: tìm slot đang 'Lên lịch' (1) mà đã đến giờ bắt đầu
    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.status = 1
              AND s.startDate <= :now
              AND s.endDate > :now
            """)
    List<FlashSaleSlot> findSlotsToActivate(@Param("now") LocalDateTime now);

    // Scheduled: tìm slot đang 'Đang diễn ra' (2) mà đã quá endDate

    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.status = 2
              AND s.endDate <= :now
            """)
    List<FlashSaleSlot> findSlotsToExpire(@Param("now") LocalDateTime now);

    // Scheduled: tìm slot 'Lên lịch' (1) mà endDate đã qua mà chưa kịp chạy

    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.status = 1
              AND s.endDate <= :now
            """)
    List<FlashSaleSlot> findOverdueScheduledSlots(@Param("now") LocalDateTime now);

    // Chặn 2 flash sale chạy cùng khung giờ
    // 2 khoảng [start, end] giao nhau khi: startA < endB AND startB < endA
    // Chỉ tính các slot ĐANG hoạt động (2) hoặc ĐÃ lên lịch (1).
    // Bỏ qua: 0 (xóa), 3 (đã kết thúc), 4 (đã hủy) vì không còn chiếm khung giờ.

    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.status IN (1, 2)
              AND s.startDate < :newEndDate
              AND s.endDate   > :newStartDate
              AND (:excludeSlotId IS NULL OR s.slotId <> :excludeSlotId)
            ORDER BY s.startDate ASC
            """)
    List<FlashSaleSlot> findOverlappingSlots(
            @Param("newStartDate") LocalDateTime newStartDate,
            @Param("newEndDate") LocalDateTime newEndDate,
            @Param("excludeSlotId") Integer excludeSlotId);
}
