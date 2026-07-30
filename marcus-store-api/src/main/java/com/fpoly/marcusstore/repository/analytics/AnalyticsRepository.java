package com.fpoly.marcusstore.repository.analytics;

import com.fpoly.marcusstore.entity.shopping.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Marcus làm: nguồn truy vấn riêng cho tab Phân tích kinh doanh.
 *
 * Repository này không phụ thuộc StatisticsRepository của Dashboard. Các truy
 * vấn
 * bán hàng dùng Orders.created_at; các truy vấn dòng tiền dùng thời điểm giao
 * dịch.
 */
public interface AnalyticsRepository extends Repository<Order, Integer> {

    @Query(value = """
            SELECT
                COUNT_BIG(*) AS totalOrders,
                COUNT_BIG(CASE WHEN o.order_status = 'COMPLETED' THEN 1 END) AS completedOrders,
                COUNT_BIG(CASE WHEN o.order_status = 'CANCELLED' THEN 1 END) AS cancelledOrders,
                COALESCE(SUM(CASE WHEN o.order_status = 'COMPLETED' THEN o.final_amount ELSE 0 END), 0)
                    AS completedSales,
                COUNT_BIG(DISTINCT CASE WHEN o.order_status = 'COMPLETED' THEN o.user_id END)
                    AS orderingCustomers,
                COALESCE((
                    SELECT SUM(oi.quantity)
                    FROM Order_Items oi
                    INNER JOIN Orders completed_order ON completed_order.order_id = oi.order_id
                    WHERE completed_order.order_status = 'COMPLETED'
                      AND completed_order.created_at >= :fromDateTime
                      AND completed_order.created_at < :toDateTimeExclusive
                ), 0) AS unitsSold
            FROM Orders o
            WHERE o.created_at >= :fromDateTime
              AND o.created_at < :toDateTimeExclusive
            """, nativeQuery = true)
    SalesSummaryProjection summarizeSales(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Order_Transactions t
            WHERE t.type = 'REFUND'
              AND t.status = 'SUCCESS'
              AND t.created_at >= :fromDateTime
              AND t.created_at < :toDateTimeExclusive
            """, nativeQuery = true)
    BigDecimal sumSuccessfulRefunds(
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            WITH DateSeries AS (
                SELECT CAST(:fromDate AS DATE) AS reportDate
                UNION ALL
                SELECT DATEADD(DAY, 1, reportDate)
                FROM DateSeries
                WHERE reportDate < CAST(:toDate AS DATE)
            ),
            CompletedOrders AS (
                SELECT
                    o.order_id,
                    CAST(o.created_at AS DATE) AS reportDate,
                    o.final_amount
                FROM Orders o
                WHERE o.order_status = 'COMPLETED'
                  AND o.created_at >= :fromDateTime
                  AND o.created_at < :toDateTimeExclusive
            ),
            UnitsByDay AS (
                SELECT
                    completed_order.reportDate,
                    SUM(oi.quantity) AS unitsSold
                FROM CompletedOrders completed_order
                INNER JOIN Order_Items oi ON oi.order_id = completed_order.order_id
                GROUP BY completed_order.reportDate
            )
            SELECT
                ds.reportDate AS reportDate,
                COALESCE(SUM(completed_order.final_amount), 0) AS completedSales,
                COUNT_BIG(completed_order.order_id) AS completedOrders,
                COALESCE(MAX(units.unitsSold), 0) AS unitsSold
            FROM DateSeries ds
            LEFT JOIN CompletedOrders completed_order
                ON completed_order.reportDate = ds.reportDate
            LEFT JOIN UnitsByDay units
                ON units.reportDate = ds.reportDate
            GROUP BY ds.reportDate
            ORDER BY ds.reportDate
            OPTION (MAXRECURSION 2000)
            """, nativeQuery = true)
    List<SalesTrendProjection> findSalesTrend(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive);

    @Query(value = """
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                p.brand AS brand,
                COALESCE(SUM(CASE
                    WHEN o.created_at >= :currentFrom
                     AND o.created_at < :currentToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS currentUnits,
                COALESCE(SUM(CASE
                    WHEN o.created_at >= :previousFrom
                     AND o.created_at < :previousToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS previousUnits,
                COALESCE(SUM(CASE
                    WHEN o.created_at >= :currentFrom
                     AND o.created_at < :currentToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS currentMerchandiseSales,
                COALESCE(SUM(CASE
                    WHEN o.created_at >= :previousFrom
                     AND o.created_at < :previousToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS previousMerchandiseSales
            FROM Order_Items oi
            INNER JOIN Orders o ON o.order_id = oi.order_id
            INNER JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
            INNER JOIN Products p ON p.product_id = sku.product_id
            WHERE o.order_status = 'COMPLETED'
              AND o.created_at >= :previousFrom
              AND o.created_at < :currentToExclusive
            GROUP BY p.product_id, p.product_name, p.brand
            HAVING SUM(CASE
                WHEN o.created_at >= :currentFrom
                 AND o.created_at < :currentToExclusive
                THEN oi.quantity ELSE 0 END) > 0
            ORDER BY currentUnits DESC, currentMerchandiseSales DESC
            """, nativeQuery = true)
    List<ProductTrendProjection> findProductTrends(
            @Param("currentFrom") LocalDateTime currentFrom,
            @Param("currentToExclusive") LocalDateTime currentToExclusive,
            @Param("previousFrom") LocalDateTime previousFrom,
            @Param("previousToExclusive") LocalDateTime previousToExclusive);

    interface SalesSummaryProjection {
        Long getTotalOrders();

        Long getCompletedOrders();

        Long getCancelledOrders();

        BigDecimal getCompletedSales();

        Long getUnitsSold();

        Long getOrderingCustomers();
    }

    interface SalesTrendProjection {
        LocalDate getReportDate();

        BigDecimal getCompletedSales();

        Long getCompletedOrders();

        Long getUnitsSold();
    }

    interface ProductTrendProjection {
        Integer getProductId();

        String getProductName();

        String getBrand();

        Long getCurrentUnits();

        Long getPreviousUnits();

        BigDecimal getCurrentMerchandiseSales();

        BigDecimal getPreviousMerchandiseSales();
    }
}
