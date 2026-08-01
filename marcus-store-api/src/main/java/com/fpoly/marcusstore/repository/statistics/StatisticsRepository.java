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

    // ── FIX 1+4: INNER JOIN thay LEFT JOIN, WHERE đúng điều kiện, tính totalProductsSold thật ──
    @Query(value = """
        WITH DateSeries AS (
            SELECT CAST(:startDate AS DATE) AS reportDate
            UNION ALL
            SELECT DATEADD(DAY, 1, reportDate)
            FROM DateSeries
            WHERE reportDate < CAST(:endDate AS DATE)
        ),
        ValidOrders AS (
            SELECT DISTINCT o.order_id, CAST(ot.created_at AS DATE) AS txDate
            FROM Orders o
            INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            WHERE ot.status = 'SUCCESS'
              AND o.order_status = 'COMPLETED'
              AND o.payment_status = 'PAID'
              AND CAST(ot.created_at AS DATE) >= CAST(:startDate AS DATE)
              AND CAST(ot.created_at AS DATE) <= CAST(:endDate AS DATE)
        )
        SELECT
            ds.reportDate                                       AS reportDate,
            COUNT(DISTINCT vo.order_id)                        AS totalOrders,
            ISNULL(SUM(oi.quantity), 0)                        AS totalProductsSold,
            ISNULL((
                SELECT SUM(ot2.amount)
                FROM Order_Transactions ot2
                INNER JOIN Orders o2 ON ot2.order_id = o2.order_id
                WHERE ot2.status = 'SUCCESS'
                  AND o2.order_status = 'COMPLETED'
                  AND o2.payment_status = 'PAID'
                  AND CAST(ot2.created_at AS DATE) = ds.reportDate
            ), 0)                                               AS totalRevenue
        FROM DateSeries ds
        LEFT JOIN ValidOrders vo ON vo.txDate = ds.reportDate
        LEFT JOIN Order_Items oi ON oi.order_id = vo.order_id
        GROUP BY ds.reportDate
        ORDER BY ds.reportDate ASC
        OPTION (MAXRECURSION 366)
        """, nativeQuery = true)
    List<RevenueByDayProjection> getRevenueByDay(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // ── FIX 1+4: KPI cũ ──
    @Query(value = """
        SELECT
            ISNULL(SUM(ot.amount), 0)       AS totalRevenue,
            COUNT(DISTINCT o.order_id)      AS totalOrders,
            ISNULL(SUM(oi.quantity), 0)     AS totalProductsSold
        FROM Orders o
        INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
        INNER JOIN Order_Items oi        ON o.order_id = oi.order_id
        WHERE ot.status = 'SUCCESS'
          AND o.order_status = 'COMPLETED'
          AND o.payment_status = 'PAID'
          AND CAST(ot.created_at AS DATE) >= :startDate
          AND CAST(ot.created_at AS DATE) <= :endDate
        """, nativeQuery = true)
    KpiSummaryProjection getKpiSummary(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // ── FIX 1+4: KPI V2 có completedOrders ──
    @Query(value = """
        WITH ValidOrders AS (
            SELECT DISTINCT o.order_id
            FROM Orders o
            INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            WHERE ot.status = 'SUCCESS'
              AND o.order_status = 'COMPLETED'
              AND o.payment_status = 'PAID'
              AND CAST(ot.created_at AS DATE) >= :startDate
              AND CAST(ot.created_at AS DATE) <= :endDate
        ),
        Revenue AS (
            SELECT ISNULL(SUM(ot2.amount), 0) AS totalRevenue
            FROM Order_Transactions ot2
            INNER JOIN ValidOrders vo2 ON ot2.order_id = vo2.order_id
            WHERE ot2.status = 'SUCCESS'
        ),
        OrderCount AS (
            SELECT COUNT(DISTINCT order_id) AS cnt FROM ValidOrders
        ),
        ProductsSold AS (
            SELECT ISNULL(SUM(oi.quantity), 0) AS totalSold
            FROM Order_Items oi
            INNER JOIN ValidOrders vo ON oi.order_id = vo.order_id
        )
        SELECT
            r.totalRevenue                    AS totalRevenue,
            oc.cnt                            AS totalOrders,
            oc.cnt                            AS completedOrders,
            ps.totalSold                      AS totalProductsSold
        FROM Revenue r, OrderCount oc, ProductsSold ps
        """, nativeQuery = true)
    KpiSummaryProjection getKpiSummaryV2(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // ── FIX 5: thống nhất trục thời gian theo ot.created_at ──
    @Query(value = """
        SELECT
            YEAR(ot.created_at)          AS reportYear,
            MONTH(ot.created_at)         AS reportMonth,
            COUNT(DISTINCT o.order_id)   AS totalOrders,
            ISNULL(SUM(oi.quantity), 0)  AS totalProductsSold,
            SUM(ot.amount)               AS totalRevenue
        FROM Order_Transactions ot
        INNER JOIN Orders o     ON ot.order_id = o.order_id
        INNER JOIN Order_Items oi ON o.order_id = oi.order_id
        WHERE ot.status = 'SUCCESS'
          AND o.order_status = 'COMPLETED'
          AND o.payment_status = 'PAID'
          AND (:year IS NULL OR YEAR(ot.created_at) = :year)
        GROUP BY YEAR(ot.created_at), MONTH(ot.created_at)
        ORDER BY reportYear DESC, reportMonth DESC
        """, nativeQuery = true)
    List<RevenueByMonthProjection> getRevenueByMonth(@Param("year") Integer year);

    // ── FIX 3: dùng CTE ValidOrders tránh nhân đôi quantity ──
    @Query(value = """
        WITH ValidOrders AS (
            SELECT DISTINCT o.order_id
            FROM Orders o
            INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            WHERE ot.status = 'SUCCESS'
              AND o.order_status = 'COMPLETED'
              AND o.payment_status = 'PAID'
              AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
              AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        )
        SELECT TOP (:topN)
            p.product_name                            AS productName,
            SUM(oi.quantity)                          AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase)   AS revenue
        FROM Order_Items oi
        INNER JOIN ValidOrders vo   ON oi.order_id  = vo.order_id
        INNER JOIN Product_Skus ps  ON oi.sku_id    = ps.sku_id
        INNER JOIN Products p       ON ps.product_id = p.product_id
        WHERE (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%')
        GROUP BY p.product_name
        ORDER BY totalSold DESC
        """, nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts(
            @Param("topN")      int topN,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("keyword")   String keyword);

    // ── FIX 1: INNER JOIN + WHERE đúng ──
    @Query(value = """
        SELECT
            DATEPART(WEEKDAY, ot.created_at) AS dayOfWeek,
            COUNT(DISTINCT o.order_id)       AS totalOrders
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
        WHERE ot.status = 'SUCCESS'
          AND o.order_status = 'COMPLETED'
          AND o.payment_status = 'PAID'
          AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
          AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        GROUP BY DATEPART(WEEKDAY, ot.created_at)
        ORDER BY dayOfWeek
        """, nativeQuery = true)
    List<OrderByWeekdayProjection> getOrdersByWeekday(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);

    // ── FIX 3+6: CTE ValidOrders tránh nhân đôi, nhãn rõ là doanh số hàng hóa ──
    @Query(value = """
        WITH ValidOrders AS (
            SELECT DISTINCT o.order_id
            FROM Orders o
            INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            WHERE ot.status = 'SUCCESS'
              AND o.order_status = 'COMPLETED'
              AND o.payment_status = 'PAID'
              AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
              AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        )
        SELECT
            p.brand                                   AS brand,
            SUM(oi.quantity)                          AS totalSold,
            SUM(oi.quantity * oi.price_at_purchase)   AS revenue
        FROM Order_Items oi
        INNER JOIN ValidOrders vo   ON oi.order_id  = vo.order_id
        INNER JOIN Product_Skus ps  ON oi.sku_id    = ps.sku_id
        INNER JOIN Products p       ON ps.product_id = p.product_id
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
               (:status = N'Het hang'     AND ps.stock_quantity = 0) OR
               (:status = N'Sap het hang' AND ps.stock_quantity > 0) OR
               (:status = N'Hết hàng'     AND ps.stock_quantity = 0) OR
               (:status = N'Sắp hết hàng' AND ps.stock_quantity > 0))
        ORDER BY ps.stock_quantity ASC
        """, nativeQuery = true)
    List<LowStockProjection> getLowStockProducts(
            @Param("threshold") int threshold,
            @Param("keyword")   String keyword,
            @Param("brand")     String brand,
            @Param("status")    String status);

    // ── FIX 1+3: CTE ValidOrders tránh nhân đôi ──
    @Query(value = """
        WITH ValidOrders AS (
            SELECT DISTINCT o.order_id, o.user_id
            FROM Orders o
            INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id
            WHERE ot.status = 'SUCCESS'
              AND o.order_status = 'COMPLETED'
              AND o.payment_status = 'PAID'
              AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate)
              AND (:endDate   IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)
        ),
        CustomerStats AS (
            SELECT
                vo.user_id,
                COUNT(DISTINCT vo.order_id)  AS totalOrders,
                SUM(ot2.amount)              AS totalSpent
            FROM ValidOrders vo
            INNER JOIN Order_Transactions ot2 ON vo.order_id = ot2.order_id
                AND ot2.status = 'SUCCESS'
            GROUP BY vo.user_id
        )
        SELECT TOP (:topN)
            u.full_name   AS customerName,
            u.email       AS email,
            cs.totalOrders,
            cs.totalSpent
        FROM CustomerStats cs
        INNER JOIN Users u ON cs.user_id = u.user_id
        WHERE (:keyword IS NULL OR u.full_name LIKE '%' + :keyword + '%'
                                OR u.email     LIKE '%' + :keyword + '%')
        ORDER BY cs.totalSpent DESC
        """, nativeQuery = true)
    List<TopCustomerProjection> getTopCustomers(
            @Param("topN")      int topN,
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate,
            @Param("keyword")   String keyword);

    // ── FIX 1: INNER JOIN + WHERE đúng ──
    @Query(value = """
        SELECT ISNULL(SUM(ot.amount), 0) AS totalRevenue
        FROM Order_Transactions ot
        INNER JOIN Orders o ON ot.order_id = o.order_id
        WHERE ot.status = 'SUCCESS'
          AND o.order_status = 'COMPLETED'
          AND o.payment_status = 'PAID'
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
        WHERE (:startDate IS NULL OR CAST(o.created_at AS DATE) >= :startDate)
          AND (:endDate   IS NULL OR CAST(o.created_at AS DATE) <= :endDate)
          AND (:keyword   IS NULL OR o.order_code     LIKE '%' + :keyword + '%'
                                  OR o.recipient_name LIKE '%' + :keyword + '%'
                                  OR o.recipient_phone LIKE '%' + :keyword + '%')
          AND (:status    IS NULL OR o.order_status = :status)
          AND (:brand     IS NULL OR EXISTS (
              SELECT 1
              FROM Order_Items oi
              INNER JOIN Product_Skus ps ON oi.sku_id    = ps.sku_id
              INNER JOIN Products p      ON ps.product_id = p.product_id
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
            o.payment_method           AS paymentMethod,
            o.order_status             AS orderStatus,
            COUNT(DISTINCT o.order_id) AS totalOrders,
            ISNULL(SUM(ot.amount), 0)  AS totalRevenue
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

    // ── FIX 1: INNER JOIN + WHERE đúng ──
    @Query(value = """
        WITH DateSeries AS (
            SELECT CAST(:startDate AS DATE) AS dt
            UNION ALL
            SELECT DATEADD(DAY, 1, dt)
            FROM DateSeries
            WHERE dt < CAST(:endDate AS DATE)
        )
        SELECT
            ds.dt                      AS dateLabel,
            ISNULL(SUM(ot.amount), 0)  AS totalRevenue
        FROM DateSeries ds
        LEFT JOIN (
            SELECT ot2.order_id, ot2.amount, CAST(ot2.created_at AS DATE) AS txDate
            FROM Order_Transactions ot2
            INNER JOIN Orders o2 ON ot2.order_id = o2.order_id
            WHERE ot2.status = 'SUCCESS'
              AND o2.order_status = 'COMPLETED'
              AND o2.payment_status = 'PAID'
        ) ot ON ot.txDate = ds.dt
        GROUP BY ds.dt
        ORDER BY ds.dt ASC
        OPTION (MAXRECURSION 366)
        """, nativeQuery = true)
    List<RevenueCompareProjection> getRevenueByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate")   LocalDate endDate);


    // Count queries for pagination
    @Query(value = "WITH ValidOrders AS (SELECT DISTINCT o.order_id FROM Orders o INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id WHERE ot.status = 'SUCCESS' AND o.order_status = 'COMPLETED' AND o.payment_status = 'PAID' AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)) SELECT COUNT(DISTINCT p.product_name) FROM Order_Items oi INNER JOIN ValidOrders vo ON oi.order_id = vo.order_id INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id INNER JOIN Products p ON ps.product_id = p.product_id WHERE (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%')", nativeQuery = true)
    Long countTopSellingProducts(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword);

    @Query(value = "SELECT COUNT(*) FROM Product_Skus ps INNER JOIN Products p ON ps.product_id = p.product_id WHERE ps.is_active = 1 AND ps.stock_quantity <= :threshold AND (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%' OR ps.sku_code LIKE '%' + :keyword + '%') AND (:brand IS NULL OR p.brand = :brand) AND (:status IS NULL OR (:status = N'H\u1EBFt h\u00E0ng' AND ps.stock_quantity = 0) OR (:status = N'S\u1EAFp h\u1EBFt h\u00E0ng' AND ps.stock_quantity > 0))", nativeQuery = true)
    Long countLowStockProducts(@Param("threshold") int threshold, @Param("keyword") String keyword, @Param("brand") String brand, @Param("status") String status);

    @Query(value = "WITH ValidOrders AS (SELECT DISTINCT o.order_id, o.user_id FROM Orders o INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id WHERE ot.status = 'SUCCESS' AND o.order_status = 'COMPLETED' AND o.payment_status = 'PAID' AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)) SELECT COUNT(DISTINCT u.user_id) FROM ValidOrders vo INNER JOIN Users u ON vo.user_id = u.user_id WHERE (:keyword IS NULL OR u.full_name LIKE '%' + :keyword + '%' OR u.email LIKE '%' + :keyword + '%')", nativeQuery = true)
    Long countTopCustomers(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword);

    @Query(value = "SELECT COUNT(*) FROM Orders o WHERE (:startDate IS NULL OR CAST(o.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(o.created_at AS DATE) <= :endDate) AND (:keyword IS NULL OR o.order_code LIKE '%' + :keyword + '%' OR o.recipient_name LIKE '%' + :keyword + '%' OR o.recipient_phone LIKE '%' + :keyword + '%') AND (:status IS NULL OR o.order_status = :status) AND (:brand IS NULL OR EXISTS (SELECT 1 FROM Order_Items oi INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id INNER JOIN Products p ON ps.product_id = p.product_id WHERE oi.order_id = o.order_id AND p.brand = :brand))", nativeQuery = true)
    Long countRecentOrders(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword, @Param("status") String status, @Param("brand") String brand);

    @Query(value = "SELECT COUNT(*) FROM Orders o WHERE o.order_status IN ('PENDING', 'PROCESSING') AND (:keyword IS NULL OR o.order_code LIKE '%' + :keyword + '%' OR o.recipient_name LIKE '%' + :keyword + '%' OR o.recipient_phone LIKE '%' + :keyword + '%')", nativeQuery = true)
    Long countPendingOrdersList(@Param("keyword") String keyword);

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
            ds.dt              AS registerDate,
            COUNT(u.user_id)   AS totalNewUsers
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

    // ── Paged queries ─────────────────────────────────────────────────────────

    @Query(value = "WITH ValidOrders AS (SELECT DISTINCT o.order_id FROM Orders o INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id WHERE ot.status = 'SUCCESS' AND o.order_status = 'COMPLETED' AND o.payment_status = 'PAID' AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)), Ranked AS (SELECT p.product_name AS productName, SUM(oi.quantity) AS totalSold, SUM(oi.quantity * oi.price_at_purchase) AS revenue, ROW_NUMBER() OVER (ORDER BY SUM(oi.quantity) DESC) AS rn FROM Order_Items oi INNER JOIN ValidOrders vo ON oi.order_id = vo.order_id INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id INNER JOIN Products p ON ps.product_id = p.product_id WHERE (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%') GROUP BY p.product_name) SELECT productName, totalSold, revenue FROM Ranked WHERE rn BETWEEN :offset + 1 AND :offset + :size", nativeQuery = true)
    List<TopProductProjection> getTopSellingProductsPaged(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Query(value = "SELECT ps.sku_code AS skuCode, p.product_name AS productName, p.brand AS brand, ps.stock_quantity AS stockQuantity FROM Product_Skus ps INNER JOIN Products p ON ps.product_id = p.product_id WHERE ps.is_active = 1 AND ps.stock_quantity <= :threshold AND (:keyword IS NULL OR p.product_name LIKE '%' + :keyword + '%' OR ps.sku_code LIKE '%' + :keyword + '%') AND (:brand IS NULL OR p.brand = :brand) AND (:status IS NULL OR (:status = N'H\u1EBFt h\u00E0ng' AND ps.stock_quantity = 0) OR (:status = N'S\u1EAFp h\u1EBFt h\u00E0ng' AND ps.stock_quantity > 0)) ORDER BY ps.stock_quantity ASC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY", nativeQuery = true)
    List<LowStockProjection> getLowStockProductsPaged(@Param("threshold") int threshold, @Param("keyword") String keyword, @Param("brand") String brand, @Param("status") String status, @Param("offset") int offset, @Param("size") int size);

    @Query(value = "WITH ValidOrders AS (SELECT DISTINCT o.order_id, o.user_id FROM Orders o INNER JOIN Order_Transactions ot ON o.order_id = ot.order_id WHERE ot.status = 'SUCCESS' AND o.order_status = 'COMPLETED' AND o.payment_status = 'PAID' AND (:startDate IS NULL OR CAST(ot.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(ot.created_at AS DATE) <= :endDate)), CustomerStats AS (SELECT vo.user_id, COUNT(DISTINCT vo.order_id) AS totalOrders, SUM(ot2.amount) AS totalSpent FROM ValidOrders vo INNER JOIN Order_Transactions ot2 ON vo.order_id = ot2.order_id AND ot2.status = 'SUCCESS' GROUP BY vo.user_id), Ranked AS (SELECT u.full_name AS customerName, u.email AS email, cs.totalOrders, cs.totalSpent, ROW_NUMBER() OVER (ORDER BY cs.totalSpent DESC) AS rn FROM CustomerStats cs INNER JOIN Users u ON cs.user_id = u.user_id WHERE (:keyword IS NULL OR u.full_name LIKE '%' + :keyword + '%' OR u.email LIKE '%' + :keyword + '%')) SELECT customerName, email, totalOrders, totalSpent FROM Ranked WHERE rn BETWEEN :offset + 1 AND :offset + :size", nativeQuery = true)
    List<TopCustomerProjection> getTopCustomersPaged(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

    @Query(value = "SELECT o.order_code AS orderCode, o.recipient_name AS customerName, o.recipient_phone AS phone, o.payment_method AS paymentMethod, o.order_status AS orderStatus, o.final_amount AS totalAmount, o.created_at AS createdAt FROM Orders o WHERE (:startDate IS NULL OR CAST(o.created_at AS DATE) >= :startDate) AND (:endDate IS NULL OR CAST(o.created_at AS DATE) <= :endDate) AND (:keyword IS NULL OR o.order_code LIKE '%' + :keyword + '%' OR o.recipient_name LIKE '%' + :keyword + '%' OR o.recipient_phone LIKE '%' + :keyword + '%') AND (:status IS NULL OR o.order_status = :status) AND (:brand IS NULL OR EXISTS (SELECT 1 FROM Order_Items oi INNER JOIN Product_Skus ps ON oi.sku_id = ps.sku_id INNER JOIN Products p ON ps.product_id = p.product_id WHERE oi.order_id = o.order_id AND p.brand = :brand)) ORDER BY o.created_at DESC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY", nativeQuery = true)
    List<RecentOrderProjection> getRecentOrdersPaged(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("keyword") String keyword, @Param("status") String status, @Param("brand") String brand, @Param("offset") int offset, @Param("size") int size);

    @Query(value = "SELECT o.order_code AS orderCode, o.recipient_name AS customerName, o.recipient_phone AS phone, o.payment_method AS paymentMethod, o.order_status AS orderStatus, o.final_amount AS totalAmount, o.created_at AS createdAt FROM Orders o WHERE o.order_status IN ('PENDING', 'PROCESSING') AND (:keyword IS NULL OR o.order_code LIKE '%' + :keyword + '%' OR o.recipient_name LIKE '%' + :keyword + '%' OR o.recipient_phone LIKE '%' + :keyword + '%') ORDER BY CASE o.order_status WHEN 'PENDING' THEN 1 WHEN 'PROCESSING' THEN 2 END, o.created_at ASC OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY", nativeQuery = true)
    List<RecentOrderProjection> getPendingOrdersPaged(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

}