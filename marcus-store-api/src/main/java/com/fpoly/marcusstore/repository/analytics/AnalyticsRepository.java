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
                (SELECT COUNT_BIG(*)
                 FROM Orders period_order
                 WHERE period_order.created_at >= :fromDateTime
                   AND period_order.created_at < :toDateTimeExclusive) AS totalOrders,
                (SELECT COUNT_BIG(*)
                 FROM Orders completed_order
                 WHERE completed_order.order_status = 'COMPLETED'
                   AND completed_order.created_at >= :fromDateTime
                   AND completed_order.created_at < :toDateTimeExclusive) AS cohortCompletedOrders,
                COUNT_BIG(DISTINCT paid.order_id) AS completedOrders,
                (SELECT COUNT_BIG(*)
                 FROM Orders cancelled_order
                 WHERE cancelled_order.order_status = 'CANCELLED'
                   AND cancelled_order.created_at >= :fromDateTime
                   AND cancelled_order.created_at < :toDateTimeExclusive) AS cancelledOrders,
                COALESCE(SUM(paid.amount), 0) AS completedSales,
                COUNT_BIG(DISTINCT paid.user_id) AS orderingCustomers,
                COALESCE((
                    SELECT SUM(oi.quantity)
                    FROM Order_Items oi
                    WHERE EXISTS (
                        SELECT 1
                        FROM Order_Transactions paid_tx
                        INNER JOIN Orders paid_order ON paid_order.order_id = paid_tx.order_id
                        WHERE paid_tx.order_id = oi.order_id
                          AND paid_tx.status = 'SUCCESS'
                          AND paid_tx.type <> 'REFUND'
                          AND paid_order.order_status = 'COMPLETED'
                          AND paid_tx.created_at >= :fromDateTime
                          AND paid_tx.created_at < :toDateTimeExclusive
                    )
                ), 0) AS unitsSold
            FROM (
                SELECT
                    t.order_id,
                    o.user_id,
                    SUM(t.amount) AS amount
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :fromDateTime
                  AND t.created_at < :toDateTimeExclusive
                GROUP BY t.order_id, o.user_id
            ) paid
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
            CompletedPaidOrders AS (
                SELECT
                    t.order_id,
                    CAST(MIN(t.created_at) AS DATE) AS reportDate,
                    SUM(t.amount) AS completedSales
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :fromDateTime
                  AND t.created_at < :toDateTimeExclusive
                GROUP BY t.order_id
            ),
            UnitsByDay AS (
                SELECT
                    completed_order.reportDate,
                    SUM(oi.quantity) AS unitsSold
                FROM CompletedPaidOrders completed_order
                INNER JOIN Order_Items oi ON oi.order_id = completed_order.order_id
                GROUP BY completed_order.reportDate
            )
            SELECT
                ds.reportDate AS reportDate,
                COALESCE(SUM(completed_order.completedSales), 0) AS completedSales,
                COUNT_BIG(completed_order.order_id) AS completedOrders,
                COALESCE(MAX(units.unitsSold), 0) AS unitsSold
            FROM DateSeries ds
            LEFT JOIN CompletedPaidOrders completed_order
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
            WITH PaidOrders AS (
                SELECT
                    t.order_id,
                    MIN(t.created_at) AS paid_at
                FROM Order_Transactions t
                INNER JOIN Orders o ON o.order_id = t.order_id
                WHERE t.status = 'SUCCESS'
                  AND t.type <> 'REFUND'
                  AND o.order_status = 'COMPLETED'
                  AND t.created_at >= :previousFrom
                  AND t.created_at < :currentToExclusive
                GROUP BY t.order_id
            )
            SELECT
                p.product_id AS productId,
                p.product_name AS productName,
                p.brand AS brand,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :currentFrom
                     AND paid.paid_at < :currentToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS currentUnits,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :previousFrom
                     AND paid.paid_at < :previousToExclusive
                    THEN oi.quantity ELSE 0 END), 0) AS previousUnits,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :currentFrom
                     AND paid.paid_at < :currentToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS currentMerchandiseSales,
                COALESCE(SUM(CASE
                    WHEN paid.paid_at >= :previousFrom
                     AND paid.paid_at < :previousToExclusive
                    THEN oi.quantity * oi.price_at_purchase ELSE 0 END), 0)
                    AS previousMerchandiseSales
            FROM Order_Items oi
            INNER JOIN PaidOrders paid ON paid.order_id = oi.order_id
            INNER JOIN Product_Skus sku ON sku.sku_id = oi.sku_id
            INNER JOIN Products p ON p.product_id = sku.product_id
            GROUP BY p.product_id, p.product_name, p.brand
            HAVING SUM(CASE
                WHEN paid.paid_at >= :currentFrom
                 AND paid.paid_at < :currentToExclusive
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

        Long getCohortCompletedOrders();

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
