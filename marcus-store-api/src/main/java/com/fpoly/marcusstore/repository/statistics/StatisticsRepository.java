package com.fpoly.marcusstore.repository.statistics;

import com.fpoly.marcusstore.entity.shopping.Order;
import com.fpoly.marcusstore.repository.shopping.TopCustomerProjection;
import com.fpoly.marcusstore.repository.shopping.TopProductProjection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<Order, Integer> {

    // Doanh thu theo ngày — chỉ transaction SUCCESS + order COMPLETED
    @Query(value = """
        WITH DateSeries AS (
            SELECT CAST(:startDate AS DATE) AS reportDate
            UNION ALL
            SELECT DATEADD(DAY, 1, reportDate)
            FROM DateSeries
            WHERE reportDate < CAST(:endDate AS DATE)
        )
        SELECT
            ds.reportDate                           AS reportDate,
            COUNT(DISTINCT o.order_id)              AS totalOrders,
            0                                        AS totalProductsSold,
            ISNULL(SUM(ot.amount), 0)               AS totalRevenue
        FROM DateSeries ds
        LEFT JOIN Order_Transactions ot
            ON CAST(ot.created_at AS DATE) = ds.reportDate
            AND ot.status = 'SUCCESS'
        LEFT JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        GROUP BY ds.reportDate
        ORDER BY ds.reportDate ASC
        OPTION (MAXRECURSION 366)
        """, nativeQuery = true)
    List<RevenueByDayProjection> getRevenueByDay(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    @Query(value = """
        SELECT
            ISNULL(SUM(ot.amount), 0)               AS totalRevenue,
            COUNT(DISTINCT o.order_id)              AS totalOrders,
            0                                        AS totalProductsSold
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        WHERE ot.status = 'SUCCESS'
            AND CAST(ot.created_at AS DATE) >= :startDate
            AND CAST(ot.created_at AS DATE) <= :endDate
        """, nativeQuery = true)
    KpiSummaryProjection getKpiSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // KPI mới — chỉ tính transaction SUCCESS + order COMPLETED
    @Query(value = """
        SELECT
            ISNULL(SUM(CASE WHEN o.order_status = 'COMPLETED' THEN ot.amount ELSE 0 END), 0)     AS totalRevenue,
            COUNT(DISTINCT o.order_id)                                                             AS totalOrders,
            COUNT(DISTINCT CASE WHEN o.order_status = 'COMPLETED' THEN o.order_id END)            AS completedOrders,
            ISNULL((
                SELECT SUM(oi.quantity)
                FROM Order_Items oi
                INNER JOIN Orders o2 ON oi.order_id = o2.order_id
                INNER JOIN Order_Transactions ot2 ON o2.order_id = ot2.order_id
                WHERE ot2.status = 'SUCCESS'
                  AND o2.order_status = 'COMPLETED'
                  AND CAST(ot2.created_at AS DATE) >= :startDate
                  AND CAST(ot2.created_at AS DATE) <= :endDate
            ), 0)                                                                                   AS totalProductsSold
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
        WHERE ot.status = 'SUCCESS'
            AND o.order_status = 'COMPLETED'
            AND CAST(ot.created_at AS DATE) >= :startDate
            AND CAST(ot.created_at AS DATE) <= :endDate
        """, nativeQuery = true)
    KpiSummaryProjection getKpiSummaryV2(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    @Query(value = """
        SELECT
            YEAR(ot.created_at)          AS reportYear,
            MONTH(ot.created_at)         AS reportMonth,
            COUNT(DISTINCT o.order_id)   AS totalOrders,
            0                             AS totalProductsSold,
            SUM(ot.amount)               AS totalRevenue
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        WHERE ot.status = 'SUCCESS'
            AND (:year IS NULL OR YEAR(ot.created_at) = :year)
        GROUP BY YEAR(ot.created_at), MONTH(ot.created_at)
        ORDER BY reportYear DESC, reportMonth DESC
        """, nativeQuery = true)
    List<RevenueByMonthProjection> getRevenueByMonth(@Param("year") Integer year);

    // Top sản phẩm — chỉ order COMPLETED + transaction SUCCESS
    @Query(value = """
        SELECT TOP (:topN)
            p.product_name                            AS productName,
            SUM(oi.quantity)                          AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase)   AS revenue
        FROM Order_Items oi
        INNER JOIN Product_Skus ps ON oi.sku_id     = ps.sku_id
        INNER JOIN Products     p  ON ps.product_id  = p.product_id
        INNER JOIN Orders       o  ON oi.order_id   = o.order_id
            AND o.order_status = 'COMPLETED'
        INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            AND ot.status = 'SUCCESS'
        WHERE (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
            AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
            AND (:keyword   IS NULL OR p.product_name LIKE '%' + :keyword + '%')
        GROUP BY p.product_name
        ORDER BY totalSold DESC
        """, nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts(
            @Param("topN")      int topN,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("keyword")   String keyword);

    // Đơn theo thứ — chỉ order COMPLETED + transaction SUCCESS
    @Query(value = """
        SELECT
            DATEPART(WEEKDAY, ot.created_at) AS dayOfWeek,
            COUNT(DISTINCT o.order_id)       AS totalOrders
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        WHERE ot.status = 'SUCCESS'
            AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
            AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        GROUP BY DATEPART(WEEKDAY, ot.created_at)
        ORDER BY dayOfWeek
        """, nativeQuery = true)
    List<OrderByWeekdayProjection> getOrdersByWeekday(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // Doanh thu theo brand — chỉ order COMPLETED + transaction SUCCESS
    @Query(value = """
        SELECT
            p.brand                                   AS brand,
            SUM(oi.quantity)                          AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase)   AS revenue
        FROM Order_Items oi
        INNER JOIN Product_Skus ps ON oi.sku_id     = ps.sku_id
        INNER JOIN Products     p  ON ps.product_id  = p.product_id
        INNER JOIN Orders       o  ON oi.order_id   = o.order_id
            AND o.order_status = 'COMPLETED'
        INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            AND ot.status = 'SUCCESS'
        WHERE (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
            AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        GROUP BY p.brand
        ORDER BY revenue DESC
        """, nativeQuery = true)
    List<BrandRevenueProjection> getRevenueByBrand(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    @Query(value = """
        SELECT
            ps.sku_code       AS skuCode,
            p.product_name    AS productName,
            p.brand           AS brand,
            ps.stock_quantity AS stockQuantity
        FROM Product_Skus ps
        INNER JOIN Products p ON ps.product_id = p.product_id
        WHERE ps.is_active = 1
            AND ps.stock_quantity <= :threshold
            AND (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%'
                                  OR ps.sku_code    LIKE '%' + :keyword + '%')
            AND (:brand   IS NULL OR p.brand = :brand)
            AND (:status  IS NULL OR
                 (:status = 'Hết hàng'     AND ps.stock_quantity = 0) OR
                 (:status = 'Sắp hết hàng' AND ps.stock_quantity > 0))
        ORDER BY ps.stock_quantity ASC
        """, nativeQuery = true)
    List<LowStockProjection> getLowStockProducts(
            @Param("threshold") int threshold,
            @Param("keyword")   String keyword,
            @Param("brand")     String brand,
            @Param("status")    String status);

    // Top khách hàng — chỉ order COMPLETED + transaction SUCCESS
    @Query(value = """
        SELECT TOP (:topN)
            u.full_name                AS customerName,
            u.email                    AS email,
            COUNT(DISTINCT o.order_id) AS totalOrders,
            SUM(ot.amount)             AS totalSpent
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        INNER JOIN Users u ON o.user_id = u.user_id
        WHERE ot.status = 'SUCCESS'
            AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
            AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
            AND (:keyword   IS NULL OR u.full_name LIKE '%' + :keyword + '%'
                                    OR u.email     LIKE '%' + :keyword + '%')
        GROUP BY u.full_name, u.email
        ORDER BY totalSpent DESC
        """, nativeQuery = true)
    List<TopCustomerProjection> getTopCustomers(
            @Param("topN")      int topN,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("keyword")   String keyword);

    // Tổng doanh thu — chỉ order COMPLETED + transaction SUCCESS
    @Query(value = """
        SELECT ISNULL(SUM(ot.amount), 0) AS totalRevenue
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        WHERE ot.status = 'SUCCESS'
            AND CAST(ot.created_at AS DATE) >= :startDate
            AND CAST(ot.created_at AS DATE) <= :endDate
        """, nativeQuery = true)
    BigDecimal getTotalRevenue(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    @Query(value = """
        SELECT TOP (:limit)
            o.order_code       AS orderCode,
            o.recipient_name   AS customerName,
            o.recipient_phone  AS phone,
            o.payment_method   AS paymentMethod,
            o.order_status     AS orderStatus,
            o.final_amount     AS totalAmount,
            o.created_at       AS createdAt
        FROM Orders o
        WHERE (:startDate   IS NULL OR CAST(o.created_at AS DATE) >= :startDate)
            AND (:endDate   IS NULL OR CAST(o.created_at AS DATE) <= :endDate)
            AND (:keyword   IS NULL OR o.order_code      LIKE '%' + :keyword + '%'
                                    OR o.recipient_name  LIKE '%' + :keyword + '%'
                                    OR o.recipient_phone LIKE '%' + :keyword + '%')
            AND (:status    IS NULL OR o.order_status = :status)
            AND (:brand     IS NULL OR EXISTS (
                SELECT 1
                FROM Order_Items oi
                INNER JOIN Product_Skus ps ON oi.sku_id    = ps.sku_id
                INNER JOIN Products     p  ON ps.product_id = p.product_id
                WHERE oi.order_id = o.order_id AND p.brand = :brand
            ))
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<RecentOrderProjection> getRecentOrders(
            @Param("limit")     int limit,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("keyword")   String keyword,
            @Param("status")    String status,
            @Param("brand")     String brand);

    @Query(value = """
        SELECT TOP (:limit)
            o.order_code       AS orderCode,
            o.recipient_name   AS customerName,
            o.recipient_phone  AS phone,
            o.payment_method   AS paymentMethod,
            o.order_status     AS orderStatus,
            o.final_amount     AS totalAmount,
            o.created_at       AS createdAt
        FROM Orders o
        WHERE o.order_status IN ('PENDING', 'PROCESSING')
            AND (:keyword IS NULL OR o.order_code      LIKE '%' + :keyword + '%'
                                  OR o.recipient_name  LIKE '%' + :keyword + '%'
                                  OR o.recipient_phone LIKE '%' + :keyword + '%')
        ORDER BY
            CASE o.order_status
                WHEN 'PENDING'    THEN 1
                WHEN 'PROCESSING' THEN 2
            END,
            o.created_at ASC
        """, nativeQuery = true)
    List<RecentOrderProjection> getPendingOrders(
            @Param("limit")   int limit,
            @Param("keyword") String keyword);

    @Query(value = """
        SELECT
            o.payment_method               AS paymentMethod,
            o.order_status                 AS orderStatus,
            COUNT(DISTINCT o.order_id)     AS totalOrders,
            ISNULL(SUM(ot.amount), 0)      AS totalRevenue
        FROM Orders o
        LEFT JOIN Order_Transactions ot ON o.order_id = ot.order_id
            AND ot.status = 'SUCCESS'
        WHERE (:startDate IS NULL OR CAST(o.created_at AS DATE) >= :startDate)
          AND (:endDate   IS NULL OR CAST(o.created_at AS DATE) <= :endDate)
        GROUP BY o.payment_method, o.order_status
        ORDER BY totalOrders DESC
        """, nativeQuery = true)
    List<PaymentStatusProjection> getPaymentStats(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // Revenue compare — chỉ transaction SUCCESS + order COMPLETED
    @Query(value = """
        WITH DateSeries AS (
            SELECT CAST(:startDate AS DATE) AS dt
            UNION ALL
            SELECT DATEADD(DAY, 1, dt)
            FROM DateSeries
            WHERE dt < CAST(:endDate AS DATE)
        )
        SELECT
            ds.dt                           AS dateLabel,
            ISNULL(SUM(ot.amount), 0)       AS totalRevenue
        FROM DateSeries ds
        LEFT JOIN Order_Transactions ot
            ON CAST(ot.created_at AS DATE) = ds.dt
            AND ot.status = 'SUCCESS'
        LEFT JOIN Orders o ON ot.order_id = o.order_id
            AND o.order_status = 'COMPLETED'
        GROUP BY ds.dt
        ORDER BY ds.dt ASC
        OPTION (MAXRECURSION 366)
        """, nativeQuery = true)
    List<RevenueCompareProjection> getRevenueByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    @Query(value = "SELECT COUNT(*) FROM Orders WHERE order_status IN ('PENDING', 'PROCESSING')",
           nativeQuery = true)
    Long countPendingOrders();

    @Query(value = """
        WITH DateSeries AS (
            SELECT CAST(:startDate AS DATE) AS dt
            UNION ALL
            SELECT DATEADD(DAY, 1, dt)
            FROM DateSeries
            WHERE dt < CAST(:endDate AS DATE)
        )
        SELECT
            ds.dt                           AS registerDate,
            COUNT(u.user_id)                AS totalNewUsers
        FROM DateSeries ds
        LEFT JOIN Users u
            ON CAST(u.created_at AS DATE) = ds.dt
            AND u.email_verified = 1
        GROUP BY ds.dt
        ORDER BY ds.dt ASC
        OPTION (MAXRECURSION 366)
        """, nativeQuery = true)
    List<NewUserByDayProjection> getNewUsersByDay(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);
}