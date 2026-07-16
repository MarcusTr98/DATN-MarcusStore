package com.fpoly.marcusstore.repository.shopping;

import com.fpoly.marcusstore.entity.shopping.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    List<OrderItem> findByOrder_OrderId(Integer orderId);

    //Đức thêm: lấy số lượng đã bán của sản phẩm
    @Query("""
                SELECT COALESCE(SUM(oi.quantity), 0)
                FROM OrderItem oi
                JOIN oi.order o
                JOIN oi.sku sku
                WHERE sku.product.productId = :productId
                  AND o.orderStatus = 'COMPLETED'
                  AND o.isHidden = false
            """)
    Long sumSoldQuantityByProductId(@Param("productId") Integer productId);
}
