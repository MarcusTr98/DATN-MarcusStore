package com.fpoly.marcusstore.repository.core;

import com.fpoly.marcusstore.entity.core.ProductItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductItemRepository
        extends JpaRepository<ProductItem, Integer>, JpaSpecificationExecutor<ProductItem> {

    List<ProductItem> findByProductSku_SkuIdOrderByItemIdDesc(Integer skuId);

    long countByProductSku_SkuIdAndStatus(Integer skuId, Integer status);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.productSku.skuId = :skuId")
    long countBySkuId(@Param("skuId") Integer skuId);

    boolean existsByImeiCode(String imeiCode);

    @Query("SELECT pi FROM ProductItem pi WHERE pi.productSku.skuId = :skuId AND pi.status = 1 AND pi.orderItem IS NULL")
    List<ProductItem> findAvailableBySkuId(@Param("skuId") Integer skuId);

    @Query("SELECT pi.imeiCode FROM ProductItem pi WHERE pi.imeiCode IN :codes")
    List<String> findExistingImeiCodes(@Param("codes") List<String> codes);

    @Query("SELECT pi FROM ProductItem pi WHERE pi.imeiCode = :imeiCode AND pi.status = 1 AND pi.orderItem IS NULL")
    ProductItem findAvailableByImeiCode(@Param("imeiCode") String imeiCode);

    @Query("SELECT pi FROM ProductItem pi WHERE pi.imeiCode IN :codes")
    List<ProductItem> findByImeiCodes(@Param("codes") List<String> codes);

    // Marcus thêm: khoá cứng các dòng IMEI để tránh hai request gán cùng IMEI
    // cho hai đơn hàng khác nhau khi có request chạy song song.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM ProductItem pi WHERE pi.imeiCode IN :codes AND pi.status = 1 AND pi.orderItem IS NULL")
    List<ProductItem> findAvailableByImeiCodesForUpdate(@Param("codes") List<String> codes);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.orderItem.orderItemId = :orderItemId")
    long countByOrderItemId(@Param("orderItemId") Integer orderItemId);

    @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.productSku.skuId = :skuId AND pi.status = 1")
    long countInStockBySkuId(@Param("skuId") Integer skuId);

    // Marcus sửa hỗ trợ module kho: lượng đơn đang giữ nhưng chưa có đủ IMEI.
    // Không tính đơn đã hủy/hoàn thành và không trừ lại các IMEI đã gán.
    @Query(value = """
            SELECT COALESCE(SUM(
                CASE
                    WHEN oi.quantity > COALESCE(assigned.assigned_qty, 0)
                    THEN oi.quantity - COALESCE(assigned.assigned_qty, 0)
                    ELSE 0
                END
            ), 0)
            FROM Order_Items oi
            INNER JOIN Orders o ON o.order_id = oi.order_id
            LEFT JOIN (
                SELECT order_item_id, COUNT(*) AS assigned_qty
                FROM Product_Items
                WHERE order_item_id IS NOT NULL
                GROUP BY order_item_id
            ) assigned ON assigned.order_item_id = oi.order_item_id
            WHERE oi.sku_id = :skuId
              AND o.order_status IN (
                  'PENDING','CONFIRMED','PROCESSING',
                  'READY_FOR_PICKUP','PACKED','SHIPPING'
              )
            """, nativeQuery = true)
    long countReservedWithoutImeiBySkuId(@Param("skuId") Integer skuId);
}
