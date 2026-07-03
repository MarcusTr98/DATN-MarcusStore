package com.fpoly.marcusstore.repository.promotion;

import com.fpoly.marcusstore.entity.promotion.FlashSaleItem;
import com.fpoly.marcusstore.entity.promotion.FlashSaleItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

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
}
