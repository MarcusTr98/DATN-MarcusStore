package com.fpoly.marcusstore.repository.statistics;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.TopCustomerProjection;
import com.fpoly.marcusstore.repository.shopping.TopProductProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<Order, Integer> {

    @Query(value = """
        SELECT
            CAST(o.created_at AS DATE) AS reportDate,
            COUNT(DISTINCT o.order_id) AS totalOrders,
            SUM(oi.quantity) AS totalProductsSold,
            SUM(o.final_amount) AS totalRevenue
        FROM Orders o
        INNER JOIN Order_Items oi ON o.order_id = oi.order_id
        WHERE o.payment_status = 'PAID'
        GROUP BY CAST(o.created_at AS DATE)
        ORDER BY reportDate DESC
        """, nativeQuery = true)
    List<RevenueByDayProjection> getRevenueByDay();

    @Query(value = """
        SELECT
            YEAR(o.created_at) AS reportYear,
            MONTH(o.created_at) AS reportMonth,
            COUNT(DISTINCT o.order_id) AS totalOrders,
            SUM(oi.quantity) AS totalProductsSold,
            SUM(o.final_amount) AS totalRevenue
        FROM Orders o
        INNER JOIN Order_Items oi ON o.order_id = oi.order_id
        WHERE o.payment_status = 'PAID'
        GROUP BY YEAR(o.created_at), MONTH(o.created_at)
        ORDER BY reportYear DESC, reportMonth DESC
        """, nativeQuery = true)
    List<RevenueByMonthProjection> getRevenueByMonth();

    @Query(value = """
        SELECT TOP (:topN)
            p.product_name AS productName,
            SUM(oi.quantity) AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase) AS revenue
        FROM Order_Items oi
        INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id
        INNER JOIN Products p ON ps.product_id = p.product_id
        INNER JOIN Orders o ON oi.order_id = o.order_id
        WHERE o.payment_status = 'PAID'
        GROUP BY p.product_name
        ORDER BY totalSold DESC
        """, nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts(@Param("topN") int topN);

    //Đơn hàng theo thứ trong tuần (7 ngày gần nhất)
    @Query(value = """
        SELECT
            DATEPART(WEEKDAY, o.created_at) AS dayOfWeek,
            COUNT(DISTINCT o.order_id) AS totalOrders
        FROM Orders o
        WHERE o.created_at >= DATEADD(DAY, -7, GETDATE())
        GROUP BY DATEPART(WEEKDAY, o.created_at)
        ORDER BY dayOfWeek
        """, nativeQuery = true)
    List<OrderByWeekdayProjection> getOrdersByWeekday();

    //Doanh thu theo thương hiệu
    @Query(value = """
        SELECT
            p.brand AS brand,
            SUM(oi.quantity) AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase) AS revenue
        FROM Order_Items oi
        INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id
        INNER JOIN Products p ON ps.product_id = p.product_id
        INNER JOIN Orders o ON oi.order_id = o.order_id
        WHERE o.payment_status = 'PAID'
        GROUP BY p.brand
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<BrandRevenueProjection> getRevenueByBrand();

    //Sản phẩm sắp hết / hết hàng
    @Query(value = """
        SELECT
            ps.sku_code AS skuCode,
            p.product_name AS productName,
            p.brand AS brand,
            ps.stock_quantity AS stockQuantity
        FROM Product_Skus ps
        INNER JOIN Products p ON ps.product_id = p.product_id
        WHERE ps.is_active = 1 AND ps.stock_quantity <= :threshold
        ORDER BY ps.stock_quantity ASC
        """, nativeQuery = true)
    List<LowStockProjection> getLowStockProducts(@Param("threshold") int threshold);

    //Khách hàng mua nhiều nhất
    @Query(value = """
        SELECT TOP (:topN)
            u.full_name AS customerName,
            u.email AS email,
            COUNT(DISTINCT o.order_id) AS totalOrders,
            SUM(o.final_amount) AS totalSpent
        FROM Orders o
        INNER JOIN Users u ON o.user_id = u.user_id
        WHERE o.payment_status = 'PAID'
        GROUP BY u.full_name, u.email
        ORDER BY totalSpent DESC
        """, nativeQuery = true)
    List<TopCustomerProjection> getTopCustomers(@Param("topN") int topN);

    // Đơn hàng gần nhất
    @Query(value = """
        SELECT TOP (:limit)
            o.order_code AS orderCode,
            o.recipient_name AS customerName,
            o.recipient_phone AS phone,
            o.payment_method AS paymentMethod,
            o.order_status AS orderStatus,
            o.final_amount AS totalAmount,
            o.created_at AS createdAt
        FROM Orders o
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<RecentOrderProjection> getRecentOrders(@Param("limit") int limit);
}