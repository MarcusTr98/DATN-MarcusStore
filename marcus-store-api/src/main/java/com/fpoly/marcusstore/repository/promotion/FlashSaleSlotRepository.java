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

    // Lấy các slot còn hiệu lực cho client:
    //   - ACTIVE (2): đang diễn ra
    //   - SCHEDULED (1): tất cả slot trong tương lai (không giới hạn 2h - lấy slot gần nhất dù xa đến đâu)
    // Service sẽ tự sort & filter theo ưu tiên hiển thị (slot đang diễn ra trước, slot sắp diễn ra sớm nhất sau).
    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE (
                  (s.status = 2 AND s.startDate <= :now AND s.endDate >= :now)
               OR (s.status = 1 AND s.startDate > :now)
            )
            ORDER BY
              CASE s.status WHEN 2 THEN 1 ELSE 2 END,
              CASE WHEN s.status = 2 THEN s.endDate ELSE s.startDate END ASC,
              s.slotId ASC
            """)
    List<FlashSaleSlot> findActiveAndUpcomingSlots(
            @Param("now") LocalDateTime now);

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


    // Cancelled: tìm slot đã bị admin hủy (4) mà vẫn còn hiệu lực hoặc sắp diễn ra trong tương lai.
    // Dùng cho client API để FE biết slot nào đã bị hủy và hiển thị modal thông báo.
    // Lọc các slot CANCELLED có khoảng thời gian overlap với [now, endWindow].
    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.status = 4
              AND s.startDate <= :endWindow
              AND s.endDate >= :now
            ORDER BY s.startDate ASC
            """)
    List<FlashSaleSlot> findCancelledSlotsInRange(
            @Param("now") LocalDateTime now,
            @Param("endWindow") LocalDateTime endWindow);
    // Kiểm tra overlap cho việc khôi phục flash sale đã hủy.
    // Tìm các slot ACTIVE (2) hoặc SCHEDULED (1) trùng với khoảng [restoreStart, restoreEnd].
    // restoreStart = thời điểm khôi phục (now), restoreEnd = endDate gốc của slot bị hủy.
    // Logic overlap: startA < endB AND startB < endA (2 khoảng giao nhau)
    @Query("""
            SELECT s FROM FlashSaleSlot s
            WHERE s.slotId <> :excludeSlotId
              AND s.status IN (1, 2)
              AND s.startDate < :restoreEnd
              AND s.endDate > :restoreStart
            ORDER BY s.startDate ASC
            """)
    List<FlashSaleSlot> findOverlappingSlotsForRestore(
            @Param("restoreStart") LocalDateTime restoreStart,
            @Param("restoreEnd") LocalDateTime restoreEnd,
            @Param("excludeSlotId") Integer excludeSlotId);
}
