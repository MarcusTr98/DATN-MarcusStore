package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItemId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, FlashSaleItemId> {
        // Lấy tất cả item thuộc 1 slot
        List<FlashSaleItem> findBySlotSlotId(Integer slotId);

        // tổng số lượng sản phẩm trong một slotID
        @Query("""
                        SELECT fi.id.slotId, COALESCE(SUM(fi.flashSaleQuantity), 0)
                        FROM FlashSaleItem fi
                        WHERE fi.id.slotId IN :slotIds
                        GROUP BY fi.id.slotId
                        """)
        List<Object[]> sumFlashSaleQuantityBySlotIds(@Param("slotIds") Collection<Integer> slotIds);

        // Tổng soldQuantity theo slotId (dùng cho cột 'Đã sử dụng' trên bảng admin)
        @Query("""
                        SELECT fi.id.slotId, COALESCE(SUM(fi.soldQuantity), 0)
                        FROM FlashSaleItem fi
                        WHERE fi.id.slotId IN :slotIds
                        GROUP BY fi.id.slotId
                        """)
        List<Object[]> sumSoldQuantityBySlotIds(@Param("slotIds") Collection<Integer> slotIds);

        // Tìm các SKU đang bị flash sale trùng khoảng thời gian
        // Lấy tất cả item của 1 slot kèm slot (dùng cho scheduled khoá số lượng)
        @Query("""
                        SELECT fi FROM FlashSaleItem fi
                        JOIN FETCH fi.slot s
                        WHERE s.slotId = :slotId
                        """)
        List<FlashSaleItem> findItemsBySlotIdWithSlot(@Param("slotId") Integer slotId);

        // Hook checkout: lấy item flash sale đang active cho 1 SKU
        // (status=2 đang diễn ra, hoặc status=1 lên lịch mà đã đến giờ)
        // Ưu tiên item đang cháy hàng (flashSaleQuantity > soldQuantity)
        @Query("""
                        SELECT fi FROM FlashSaleItem fi
                        JOIN fi.slot s
                        WHERE fi.id.skuId = :skuId
                          AND s.status IN (1, 2)
                          AND s.startDate <= :now
                          AND s.endDate > :now
                          AND fi.flashSaleQuantity > fi.soldQuantity
                        ORDER BY s.startDate ASC
                        """)
        List<FlashSaleItem> findActiveFlashSaleItemBySku(
                @Param("skuId") Integer skuId,
                @Param("now") LocalDateTime now);

        /**
         * Khoá dòng FlashSaleItem tại thời điểm checkout (PESSIMISTIC_WRITE) để
         * ngăn 2 request cùng đọc soldQuantity rồi cộng dồn vượt flashSaleQuantity,
         * gây lỗi CHECK constraint CK_FlashSaleItems_Qty.
         *
         * QUAN TRỌNG: Lọc bỏ slot CANCELLED (status=4) để chặn đặt hàng khi admin
         * đã hủy Flash Sale nhưng user vẫn còn sản phẩm trong giỏ.
         * Slot hợp lệ: SCHEDULED (1) hoặc ACTIVE (2).
         * Nếu slot bị hủy → trả Optional.empty() → service ném 409.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT fi FROM FlashSaleItem fi
                        JOIN fi.slot s
                        WHERE fi.id.slotId = :slotId
                          AND fi.id.skuId = :skuId
                          AND s.status IN (1, 2)
                        """)
        Optional<FlashSaleItem> findForUpdate(@Param("slotId") Integer slotId,
                                              @Param("skuId") Integer skuId);

        // Marcus thêm: Dùng khi hủy đơn: phải hoàn soldQuantity kể cả slot đã kết
        // thúc/hủy.
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
                        SELECT fi FROM FlashSaleItem fi
                        JOIN FETCH fi.slot s
                        WHERE fi.id.slotId = :slotId
                          AND fi.id.skuId = :skuId
                        """)
        Optional<FlashSaleItem> findForRestore(@Param("slotId") Integer slotId,
                                               @Param("skuId") Integer skuId);
}
